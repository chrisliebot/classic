package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
public class ChrislieDispatcher {
	
	private static final String DISPATCHER_PATTERN_GROUP_ALIAS = "alias";
	private static final String DISPATCHER_PATTERN_GROUP_ARGUMENT = "argument";
	
	private Chrisliebot chrisliebot;
	
	private volatile boolean shutdown;
	
	private final AtomicInteger shutdownCounter = new AtomicInteger(0);
	
	private LoadingCache<String, Pattern> patternCache = CacheBuilder.newBuilder()
			.maximumSize(10)
			.build(new CacheLoader<>() {
				@Override
				public Pattern load(String key) {
					return Pattern.compile(key);
				}
			});
	
	private ContextResolver resolver;
	
	public ChrislieDispatcher(@NonNull Chrisliebot chrisliebot, @NonNull ContextResolver resolver) {
		this.chrisliebot = chrisliebot;
		this.resolver = resolver;
	}
	
	/**
	 * Shuts down this dispatcher and blocks until all incoming messages have been processed. Any incoming messages
	 * after a shutdown will be discarded.
	 */
	public void shutdown() throws InterruptedException {
		shutdown = true; // discard all new messages
		
		// wait for other threads to exit dispatcher section
		synchronized (shutdownCounter) {
			while (shutdownCounter.get() != 0)
				shutdownCounter.wait();
		}
	}
	
	/**
	 * Public sink method of this dispatcher. Once called, the dispatcher will build the context of the given message
	 * and dispatch it to all mapped listeners and commands.
	 *
	 * @param m The message that should be dispatched.
	 */
	public void dispatch(@NonNull ChrislieMessage m) {
		try {
			shutdownCounter.incrementAndGet();
			
			if (shutdown)
				return;
			
			var ctx = resolver.resolve(Selector::check, m);
			
			// dispatcher can be directly controlled via certain group config flax
			if (ctx.flexConf().isSet(FlexConf.DISPATCHER_DISABLE)) {
				log.trace("dispatcher is disabled for message: {}", m);
				return;
			}
			
			var parse = m.forcedInvocation();
			parse = parse.or(() -> parseCommand(m, ctx));
			var listener = parse.flatMap(p -> handleCommandInvocation(m, ctx, p));
			var isCommand = listener.isPresent();
			
			// notify all listeners
			for (var ref : ctx.listeners().values()) {
				
				// if a listener was called as a command, it will not be called again for the very same message
				if (isCommand && listener.get() == ref.envelope().listener())
					continue;
				
				// listeners can be disabled with the same flag that disables the command dispatcher
				if (ref.flexConf().isSet(FlexConf.DISPATCHER_DISABLE)) {
					log.trace("listener `{}` is disabled for message: {}", ref.envelope().source(), m);
					continue;
				}
				
				// message object contains ref, so can't be shared with all listeners, sad heap allocation :(
				var exceptionHandler = new ListenerMessageExceptionHandler();
				var lm = new ChrislieListener.ListenerMessage(
						exceptionHandler,
						chrisliebot,
						m,
						ref,
						ctx
				);
				exceptionHandler.msg = lm; // TODO: do we want to do this properly?
				
				log.trace("calling listener `{}` for message: {}", ref.envelope().source(), m);
				try {
					ref.envelope().listener().onMessage(lm, isCommand);
				} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
					exceptionHandler.escalateException(e);
				}
			}
		} finally {
			synchronized (shutdownCounter) {
				shutdownCounter.decrementAndGet();
				shutdownCounter.notifyAll();
			}
		}
	}
	
	
	private Optional<CommandParse> parseCommand(ChrislieMessage m, ChrislieContext ctx) {
		var flexConf = ctx.flexConf();
		var patternStr = flexConf.getString(FlexConf.DISPATCHER_PATTERN);
		if (patternStr.isEmpty()) {
			log.trace("no dispatcher pattern set, message will be ignored, message was: {}", m);
			return Optional.empty();
		}
		
		Pattern pattern;
		try {
			pattern = patternCache.get(patternStr.get());
		} catch (ExecutionException e) {
			log.warn("failed to compile dispatcher pattern `{}` for message: {}", patternStr.get(), m, e);
			return Optional.empty();
		}
		
		// patterns are supposed to have two groups: alias, argument
		var matcher = pattern.matcher(m.message());
		
		String alias, argument;
		if (matcher.find()) {
			
			// matcher doesn't offer to check for groups, so we have to catch the exception
			try {
				alias = matcher.group(DISPATCHER_PATTERN_GROUP_ALIAS);
				argument = matcher.group(DISPATCHER_PATTERN_GROUP_ARGUMENT);
			} catch (IllegalArgumentException e) {
				log.warn("dispatcher pattern `{}` does not contain `{}` and `{}` group, message was: {}",
						DISPATCHER_PATTERN_GROUP_ALIAS, DISPATCHER_PATTERN_GROUP_ARGUMENT, pattern.pattern(), m);
				return Optional.empty();
			}
		} else {
			log.trace("dispatcher pattern `{}` did not match on message: {}", pattern.pattern(), m);
			return Optional.empty();
		}
		
		if (alias == null || alias.isBlank()) {
			log.trace("dispatcher pattern `{}` returned empty alias on message: {}", pattern.pattern(), m);
			return Optional.empty();
		}
		alias = alias.toLowerCase();
		
		// there is no reason to differentiate between an empty argument or a null argument, so we make sure it is never null to simplify command logic
		argument = argument == null ? "" : argument;
		
		return Optional.of(new CommandParse(alias, argument));
	}
	
	/**
	 * This method attempts to execute the given message as a command. The success of this attempt depends on the
	 * pattern that is present in the resolved context and if the command name can be resolved to an alias in the
	 * context.
	 *
	 * @param m   The message that will be used for parsing.
	 * @param ctx The context of this message.
	 * @return The listener that was executed if this was a valid command invocation. This is required as listeners that
	 * were part of a command invocation will not be called a second time as a regular listener.
	 */
	private Optional<ChrislieListener> handleCommandInvocation(ChrislieMessage m, ChrislieContext ctx, CommandParse parse) {
		var alias = parse.alias();
		var args = parse.args();
		
		// lookup alias in context
		var maybeRef = ctx.alias(alias);
		if (maybeRef.isEmpty()) {
			log.trace("there is no such alias named `{}` in context, message was: {}", alias, m);
			return Optional.empty();
		}
		var ref = maybeRef.get();
		
		// listener might be disabled and should not be called
		if (ref.flexConf().isSet(FlexConf.DISPATCHER_DISABLE)) {
			log.trace("listener `{}` for alias `{}` is disabled, message was: {}", ref.envelope().source(), alias, m);
			return Optional.empty();
		}
		
		// config loader ensures that all listener with alias are command
		ChrislieListener.Command listener = (ChrislieListener.Command) ref.envelope().listener();
		
		// invocation is packed into object to keep argument list short and allow for easy adding of new data to invocation without reworking every command
		var exceptionHandler = new InvocationExceptionHandler();
		var invocation = new ChrislieListener.Invocation(
				exceptionHandler,
				chrisliebot,
				m,
				maybeRef.get(),
				ctx,
				args,
				alias
		);
		exceptionHandler.invc = invocation; // TODO: do we want to do this properly?
		
		try {
			listener.execute(invocation);
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
			exceptionHandler.escalateException(e);
		}
		
		/* the listener logic relies on knowing if a message does trigger a command, so even if
		 * the command failed inside the try-catch-block we still consider it a valid mapping
		 */
		return Optional.of(listener);
	}
	
	private static class ListenerMessageExceptionHandler implements ChrislieListener.ExceptionHandler {
		
		private ChrislieListener.ListenerMessage msg;
		
		@Override
		public void escalateException(ChrislieListener.@NonNull ListenerException e) {
			escalateException((Exception) e);
		}
		
		public void escalateException(Exception e) {
			log.error("listener callback failed with exception: {}", msg, e);
		}
	}
	
	private static class InvocationExceptionHandler implements ChrislieListener.ExceptionHandler {
		
		private ChrislieListener.Invocation invc;
		
		@Override
		public void escalateException(ChrislieListener.@NonNull ListenerException e) {
			escalateException((Exception) e);
		}
		
		public void escalateException(Exception e) {
			log.error("command invocation failed with exception: {}", invc, e);
			
			// forced invocations should not send error messages as there is no user expecting feedback
			if (invc.msg().forcedInvocation().isPresent()) {
				return;
			}
			
			// if verbose mode is enabled, we output generic error message (note that we are not using the exception text, as it might contain private information)
			if (invc.ref().flexConf().isSet(FlexConf.DISPATCHER_VERBOSE)) {
				try {
					ErrorOutputBuilder.generic("Da ging etwas schief. Das tut mir leid.").write(invc).send();
				} catch (ChrislieListener.ListenerException ex) {
					log.warn("unable to instance reply instance for dispatcher error", ex);
				}
			}
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class CommandParse {
		private String alias, args;
	}
}
