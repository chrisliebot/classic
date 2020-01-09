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
				public Pattern load(String key) throws Exception {
					return Pattern.compile(key);
				}
			});
	
	private ContextResolver resolver;
	
	public ChrislieDispatcher(@NonNull Chrisliebot chrisliebot, @NonNull ContextResolver resolver) {
		this.chrisliebot = chrisliebot;
		this.resolver = resolver;
	}
	
	/**
	 * Shuts down this dispatcher and blocks until all incoming messages have been processed. Any incoming messages after a shutdown will be discarded.
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
	 * Public sink method of this dispatcher. Once called, the dispatcher will build the context of the given message and dispatch it to all mapped listeners and
	 * commands.
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
			
			var listener = handleCommandInvocation(m, ctx);
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
				var lm = new ChrislieListener.ListenerMessage(
						chrisliebot,
						m,
						ref,
						ctx
				);
				
				log.trace("calling listener `{}` for message: {}", ref.envelope().source(), m);
				try {
					ref.envelope().listener().onMessage(lm, isCommand);
				} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
					log.error("listener callback failed with exception: {}", lm, e);
				}
			}
		} finally {
			synchronized (shutdownCounter) {
				shutdownCounter.decrementAndGet();
				shutdownCounter.notifyAll();
			}
		}
	}
	
	/**
	 * This method attempts to execute the given message as a command. The success of this attempt depends on the pattern that is present in the resolved context and if
	 * the command name can be resolved to an alias in the context.
	 *
	 * @param m   The message that will be used for parsing.
	 * @param ctx The context of this message.
	 * @return The listener that was executed if this was a valid command invocation. This is required as listeners that were part of a command invocation will not be
	 * called a second time as a regular listener.
	 */
	private Optional<ChrislieListener> handleCommandInvocation(ChrislieMessage m, ChrislieContext ctx) {
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
				log.warn("dispatcher pattern `{}` does not contain `{}` and `{}}` group, message was: {}",
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
		
		// there is no reason to differentiate between an empty argument or a null argument, so we make sure it is never null to simplify command logic
		argument = argument == null ? "" : argument;
		
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
		var invocation = new ChrislieListener.Invocation(
				chrisliebot,
				m,
				maybeRef.get(),
				ctx,
				argument,
				alias
		);
		
		try {
			listener.execute(invocation);
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
			log.error("command invocation failed with exception: {}", invocation, e);
			
			// if verbose mode is enabled, we output generic error message (note that we are not using the exception text, as it might contain private information)
			if (flexConf.isSet(FlexConf.DISPATCHER_VERBOSE)) {
				try {
					ErrorOutputBuilder.generic("Da ging etwas schief. Das tut mir leid.").write(invocation).send();
				} catch (ChrislieListener.ListenerException ex) {
					log.warn("unable to instance reply instance for dispatcher error", ex);
				}
			}
		}
		
		/* the listener logic relies on knowing if a message does trigger a command, so even if
		 * the command failed inside the try-catch-block we still consider it a valid mapping
		 */
		return Optional.of(listener);
	}
}
