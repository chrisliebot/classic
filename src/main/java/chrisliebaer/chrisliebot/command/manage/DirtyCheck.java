package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DirtyCheck implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Status des Dirty Flags: " + bot.dirty());
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
