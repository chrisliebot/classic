package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;

@FunctionalInterface
public interface ChrisieCommand {
	
	public void execute(ChrislieMessage m, String arg);
	
	/**
	 * Can be overriden to make admin only commands. More fine truned controll can be achieved by checking for admin status inside the actual command handler.
	 *
	 * @return {@code true} if this command is for admin only or {@code flase} if not.
	 */
	public default boolean requireAdmin() {
		return false;
	}
	
	public default void init(ChrislieService service) throws Exception {}
	
	public default void start() throws Exception {}
	
	public default void stop() throws Exception {}
}
