package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RestartCommand implements CommandExecutor {

	private ChrisliebotIrc chrisliebot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Führe Neustart aus...");
		log.info(C.LOG_IRC, "restarting, triggered by {}", m.user().getNick());
		chrisliebot.doShutdown(C.EXIT_CODE_RESTART);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
