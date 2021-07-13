package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import chrisliebaer.chrisliebot.command.ChrislieListener;

import java.util.Optional;

public interface ChrislieMessage extends ServiceAttached {
	
	public ChrislieChannel channel();
	
	public ChrislieUser user();
	
	public String message();
	
	/**
	 * Forced invocations are special means of triggering commands. Usually done by service specific means and bypassing the usual prefix detection.
	 *
	 * @return A possible {@link ChrislieDispatcher.CommandParse} object.
	 */
	public default Optional<ChrislieDispatcher.CommandParse> forcedInvocation() {
		return Optional.empty();
	}
	
	/**
	 * Helper method for providing a quick response without having to deal with the abstraction layer.
	 *
	 * @param limiter The LimiterConfig that will be applied to the internal output instance.
	 * @param s       The string to reply with.
	 */
	public default void reply(LimiterConfig limiter, String s) throws ChrislieListener.ListenerException {
		reply(limiter).plain(s).send();
	}
	
	/**
	 * Helper method quickly creating a reply to a received message.
	 *
	 * @param limiter The LimiterConfig that will be applied to returned output instance.
	 * @return An ouput instance for the channel that this message was received in.
	 */
	public default ChrislieOutput reply(LimiterConfig limiter) {
		return channel().output(limiter);
	}
}
