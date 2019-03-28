package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.abstraction.Message;
import org.kitteh.irc.client.library.Client;

@FunctionalInterface
public interface CommandExecutor {
	
	public void execute(Message m, String arg);
	
	/**
	 * Can be overriden to make admin only commands. More fine truned controll can be achieved by checking for admin status in {@link Message}.
	 *
	 * @return {@code true} if this command is for admin only or {@code flase} if not.
	 */
	public default boolean requireAdmin() {
		return false;
	}
	
	public default void init(Client client) throws Exception {}
	
	public default void start() throws Exception {}
	
	public default void stop() throws Exception {}
}
