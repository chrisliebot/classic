package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DirtyCheck implements ChrisieCommand {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Status des Dirty Flags: " + bot.dirty());
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
