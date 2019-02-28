package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ReconnectCommand implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Führe Reconnect aus, bis bald.");
		log.info(C.LOG_IRC, "reconnecting to host, triggered by {}", m.user().getNick());
		bot.doReconect();
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
