package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

public class SayCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		
		IrcService.run(m.service(), ircService -> {
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
			
			ircService.client().sendMultiLineMessage(target, msg);
		});
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
