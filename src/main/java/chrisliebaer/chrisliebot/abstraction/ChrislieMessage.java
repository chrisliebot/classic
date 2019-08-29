package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.command.ChrislieListener;

public interface ChrislieMessage extends ServiceAttached {
	
	public ChrislieChannel channel();
	
	public ChrislieUser user();
	
	public String message();
	
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
