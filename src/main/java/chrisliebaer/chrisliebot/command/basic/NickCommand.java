package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

public class NickCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		IrcService.run(m, ircService -> ircService.client().setNick(arg));
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
