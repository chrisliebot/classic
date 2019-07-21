package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.BotManagment;
import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RestartCommand implements ChrisieCommand {

	private BotManagment chrisliebot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Führe Neustart aus...");
		log.info(C.LOG_PUBLIC, "restarting, triggered by {}", m.user().displayName());
		chrisliebot.doShutdown(C.EXIT_CODE_RESTART);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
