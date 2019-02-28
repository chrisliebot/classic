package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpgradeCommand implements CommandExecutor {

	private ChrisliebotIrc chrisliebot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Führe Upgrade aus. Bitte prüfe in ein paar Sekunden, ob ich mich irgendwie anders verhalte. Ich hoffe das tut nicht weh ≧☉_☉≦");
		log.info(C.LOG_IRC, "performing upgrade, triggered by {}", m.user().getNick());
		chrisliebot.doShutdown(C.EXIT_CODE_UPGRADE);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
