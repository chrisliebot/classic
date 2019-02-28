package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ShutdownCommand implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Beende Anwendung, bye bye...");
		bot.doShutdown(0);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
