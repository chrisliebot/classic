package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReconnectCommand implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Führe Reconnect aus, bis bald.");
		bot.doReconect();
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
