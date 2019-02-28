package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

public class SayCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		var args = arg.split(" ", 2);
		if (args.length <= 1 || args[1].isEmpty()) {
			m.reply(C.error("Nicht genug Parameter."));
			return;
		}
		
		String target = args[0].trim();
		String msg = args[1].trim();
		if (target.isEmpty() || msg.isEmpty()) {
			m.reply(C.error("Nicht genug Parameter."));
			return;
		}
		
		m.getClient().sendMultiLineMessage(target, msg);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
