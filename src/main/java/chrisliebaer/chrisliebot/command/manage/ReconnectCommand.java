package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ReconnectCommand implements ChrisieCommand {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Führe Reconnect aus, bis bald.");
		log.info(C.LOG_PUBLIC, "reconnecting to host, triggered by {}", m.user().displayName());
		bot.doReconect();
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
