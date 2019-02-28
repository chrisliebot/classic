package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

public class NickCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		m.getClient().setNick(arg);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
