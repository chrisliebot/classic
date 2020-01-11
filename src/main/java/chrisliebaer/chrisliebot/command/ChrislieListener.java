package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.*;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.builder.ToStringExclude;

import javax.annotation.CheckReturnValue;
import java.util.Optional;

/**
 * Listeners are a core component of Chrisliebots architecture. Pretty much every protocol interaction is handled by a listener. In generel, a listener, implementing this
 * interface, is mapped into multiple scopes and notified whenever an event that Chrisliebot can handle is captured. Due to the flexibility of this interface, it is
 * important that listeners adhere to the interface contract as stated in this documentation. In order to understand how listeners are actually configured, check out the
 * documentation on configuring listeners and commands.
 *
 * <p>
 * It's important that the configuration process requires each listener to have an empty default contructor. Listeners must not perform ANY actions in their default
 * constructor. The default constructor is called during setup to create an instance of this listener. Listeners must be able to be instanced multiple times and may be
 * called from various threads concurrently. If a listener wishes to be called sequentially it must deploy proper synchronisation.
 * </p>
 *
 * <p>
 * The lifecycle of a listener is as follows:
 * <ol>
 *     <li>call to default constructor</li>
 *     <li>call to {@link #fromConfig(GsonValidator, JsonElement)} with user provided config</li>
 *     <li>call to {@link #init(Chrisliebot, ContextResolver)}</li>
 *     <li>call to {@link #start(Chrisliebot, ContextResolver)}</li>
 *     <li>listener is considered active and may receive callbacks</li>
 *     <li>call to {@link #stop(Chrisliebot, ContextResolver)}</li>
 *     <li>listener is considered destroyed and must be ready to be garbage collected</li>
 * </ol>
 */
public interface ChrislieListener {
	
	/**
	 * This method is called during listener setup. A listener must not register itself anywhere or start any threads as this listener may still be discarded without
	 * notice.
	 *
	 * @param gson A shared {@link GsonValidator} instance that can be used by the listener to deserialize objects from the provided {@link JsonElement}.
	 * @param json The JsonElement that represents the static config in the listener definition. The exact layout of this data is part of the listener documentation and
	 *             is simply passed through by the config load.
	 * @throws ListenerException If the listener encounters any unrecoverable errors such as missing config values or malformed data. This will cause the config load to
	 *                           fail. Throwing this exception will not set the dirty bit.
	 */
	public default void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {}
	
	/**
	 * This method is called during listener initialization. In this phase listeners are allowed to set up and precompute data. The bot instance has been fully configured
	 * but the listener and command system must only be accessed via the provided ContextResolver. Note that failure in this phase must not result in lingering threads or
	 * callbacks. In other words: the instance might be discarded without further notice and must be garbage collectible without leaking ressources. It advisible to move
	 * as much work from the {@link #fromConfig(GsonValidator, JsonElement)} as possible in this phase as it is independent from the underlying config format. After this
	 * method returns, the listener may be called from other listeners and code for additional setup.
	 *
	 * @param bot      Reference to bot instance.
	 * @param resolver The resolver that is managing this listener. Note that this will be different from the bots current resolver as this listener is not yet part of
	 *                 the bot instance.
	 * @throws ListenerException If listener was unable to perform initialization. This will cause the bot to discard the created configuration and reuse the last
	 *                           existing config. Throwing this exception will not set the dirty bit.
	 */
	public default void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {}
	
	/**
	 * This method is called during startup. In this phase the bot instance is fully configured and all listeners have finished their {@link #init(Chrisliebot,
	 * ContextResolver)} phase. The bot instance has been fully configured but the listener and command system must only be accessed via the provided ContextResolver.
	 * Listener are expected to set up threads, timer, sockets or other ressources that might be lingering.
	 *
	 * @param bot      Reference to bot instance.
	 * @param resolver The resolver that is managing this listener. Note that this will be different from the bots current resolver as this listener is not yet part of *
	 *                 the bot instance.
	 * @throws ListenerException If listener was unabble to perform start. This will cause the bot to abort the current startup and get rid of all other listeners there
	 *                           were part of this process. Throwing anything but this exception will cause the dirty bit to be set.
	 */
	public default void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {}
	
	
	/**
	 * This method is called during shutdown. In this phase the bot instance is still fully configured but other listeners may have already stopped. Since this might be
	 * part of a reload, accessing the bots listener and command system is not allowed and every action has to be performed on the given ContextResolver. It is possible
	 * that a listeners will be asked to stop directly after receiving a call to {@link #start(Chrisliebot, ContextResolver)}. A listener that has thrown a {@link
	 * ListenerException} or any other exception during it's start will not have it's stop method called. The listener is expected to perform cleanup itself after
	 * creating a boo boo.
	 *
	 * @param bot      Reference to bot instance.
	 * @param resolver The resolver that is managing this listener. Note that this will be different from the bots current resolver as this listener is not yet part of *
	 *                 the bot instance.
	 * @throws ListenerException If the listener was unable to perform shutdown. Throwing this exception or any other exception will cause the dirty bit to be set.
	 */
	public default void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {}
	
	/**
	 * Called if this listener was mapped into a scope that received a message.
	 *
	 * @param msg       The message that was received along additional data about the resolved context.
	 * @param isCommand Set to {@code true} if this message triggered a command execution. Most listeners probably don't want to react to messages that triggered a
	 *                  command.
	 * @throws ListenerException Indicates that the listener was unable to function properly and user intervention is required to resolve the problem.
	 */
	public default void onMessage(ListenerMessage msg, boolean isCommand) throws ListenerException {}
	
	/**
	 * This subinterface can be implemented if a listener wishes to be also registered as an command. Command listeners get a special call to {@link #execute(Invocation)}
	 * if the alias they are mapped to is used in a command invocation.
	 *
	 * <p>
	 * Note that a listener that is part of a command invocation will NOT receive a call to its {@link #onMessage(ListenerMessage, boolean)} method for the very same
	 * message that triggered the call to {@link #execute(Invocation)}. If such behavior is required, the listener must make the call by itself.
	 * </p>
	 */
	public interface Command extends ChrislieListener {
		
		/**
		 * Allows listeners to provide a hard coded help text. Note that the returned help text should not include alias names.
		 *
		 * @param ctx The context for which the help should be provided. Note that implementations of this method must not fetch their own help from their {@link
		 *            ListenerReference} inside the context. Instead, this method is for commands that perform dynamic dispatch, to lookup the help of the referenced
		 *            command.
		 * @param ref The aggregated reference of this command in the current context. Commands with dynamic dispatch may need this to access their own reference.
		 * @return An optional help message that explains how this command can be used.
		 * @throws ListenerException If accessing the help failed.
		 */
		public default Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {return Optional.empty();}
		
		/**
		 * This method is called if this command was referenced by an alias in a command invocation. Calls to this method are exclusive in that no other command will
		 * receive the same call for the same message.
		 *
		 * @param invc An Invocation object, containing the details of this invocation.
		 * @throws ListenerException Indicates that the listener was unable to function properly and user intervention is required to resolve the problem.
		 */
		public void execute(Invocation invc) throws ListenerException;
	}
	
	/**
	 * This class is used by the listener system to attach internal information to listeners.
	 */
	@AllArgsConstructor
	@ToString
	public static class Envelope {
		
		@Getter private final @NonNull ChrislieListener listener;
		@Getter private final @NonNull String source;
	}
	
	/**
	 * Thrown to indicate failure in listener.
	 */
	public static class ListenerException extends Exception {
		
		public ListenerException(String message) {
			super(message);
		}
		
		public ListenerException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ListenerException(Throwable cause) {
			super(cause);
		}
	}
	
	@ToString
	public static class ListenerMessage implements ServiceAttached {
		
		public ListenerMessage(@NonNull ExceptionHandler exceptionHandler,
							   @NonNull Chrisliebot bot,
							   @NonNull ChrislieMessage msg,
							   @NonNull ListenerReference ref,
							   @NonNull ChrislieContext ctx) {
			this.exceptionHandler = exceptionHandler;
			this.bot = bot;
			this.msg = msg;
			this.ref = ref;
			this.ctx = ctx;
		}
		
		/**
		 * The bot instance that is powering this mess.
		 */
		@Getter @ToStringExclude private final @NonNull Chrisliebot bot;
		
		/**
		 * The message that resulted in this invocation.
		 */
		@Getter private final @NonNull ChrislieMessage msg;
		
		/**
		 * The combined reference that resolved to this listener.
		 */
		@Getter private final @NonNull ListenerReference ref;
		
		/**
		 * The context that matches the source of the command.
		 */
		@Getter private final @NonNull ChrislieContext ctx;
		
		/**
		 * A lot of listeners rely on asynchronous operations to complete their task. While synchronous code can simply throw a {@link ListenerException} to rais an
		 * error, asynchronous code can not. In order to still maintain relationship between an invocation an the potential error raised, listeners can use this exception
		 * handler to feed back their errors into the dispatcher.
		 */
		@Getter private final @NonNull ExceptionHandler exceptionHandler;
		
		@Override
		public ChrislieService service() {
			return msg.service();
		}
		
		/**
		 * Shortcut method for creating an output instance inside of listener callbacks. Calling this method will use the current ref's {@link FlexConf} to create a
		 * {@link LimiterConfig} that is then used by the returned output.
		 *
		 * @return A output that can be used to reply to the source of this listener callback. This might not be the same channel that this message came from.
		 * @throws ListenerException If the {@link FlexConf} of the current ref did not contain a valid {@link LimiterConfig} instance.
		 */
		@CheckReturnValue
		public ChrislieOutput reply() throws ListenerException {
			return msg.reply(LimiterConfig.of(ref().flexConf()));
		}
		
		/**
		 * Shortcut method for creating an quickly replying to a received message without using the abstraction layer directly.
		 *
		 * @throws ListenerException If the {@link FlexConf} of the current ref did not contain a valid {@link LimiterConfig} instance.
		 */
		public void reply(String s) throws ListenerException {
			reply().plain(s).send();
		}
	}
	
	/**
	 * This interface allows asynchronous code to feed back listener exception into the dispatcher, allowing to identify which invocations caused an exception to occur.
	 */
	public interface ExceptionHandler {
		
		/**
		 * Sends the given to the dispatcher for error handling.
		 *
		 * @param e The exception that was raised during execution.
		 */
		public void escalateException(@NonNull ListenerException e);
		
		/**
		 * Executes the given code block while propagatin all raised {@link ListenerException}s back to the dispatcher. Ideally this should be used to wrap the top level
		 * entry point of any asynchronous code.
		 *
		 * @param fn The function to wrap.
		 */
		public default void unwrap(@NonNull ListenerRunnable fn) {
			try {
				fn.run();
			} catch (ListenerException e) {
				escalateException(e);
			}
		}
	}
	
	/**
	 * Regular runnable but it can throw {@link ListenerException}.
	 */
	public interface ListenerRunnable {
		
		public void run() throws ListenerException;
	}
	
	@ToString(callSuper = true)
	public static class Invocation extends ListenerMessage {
		
		public Invocation(@NonNull ExceptionHandler exceptionHandler,
						  @NonNull Chrisliebot bot,
						  @NonNull ChrislieMessage msg,
						  @NonNull ListenerReference ref,
						  @NonNull ChrislieContext ctx,
						  @NonNull String arg,
						  @NonNull String alias) {
			super(exceptionHandler, bot, msg, ref, ctx);
			this.arg = arg;
			this.alias = alias;
		}
		
		/**
		 * The argument that was passed along the command. Never null.
		 */
		@Getter private final @NonNull String arg;
		/**
		 * The alias that was used to access this command.
		 */
		@Getter private final @NonNull String alias;
		
		
		public Invocation arg(@NonNull String arg) {
			return new Invocation(
					exceptionHandler(),
					bot(),
					msg(),
					ref(),
					ctx(),
					arg,
					alias());
		}
		
		public Invocation ref(@NonNull ListenerReference ref) {
			return new Invocation(
					exceptionHandler(),
					bot(),
					msg(),
					ref,
					ctx(),
					arg(),
					alias());
		}
	}
}
