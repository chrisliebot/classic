package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ShutdownCommand implements ChrisieCommand {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Beende Anwendung, bye bye...");
		log.info(C.LOG_PUBLIC, "shutting down, triggered by {}", m.user().displayName());
		bot.doShutdown(0);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
