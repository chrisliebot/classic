package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ShutdownCommand implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Beende Anwendung, bye bye...");
		log.info(C.LOG_IRC, "shutting down, triggered by {}", m.user().getNick());
		bot.doShutdown(0);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
