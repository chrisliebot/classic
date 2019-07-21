package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.BotManagment;
import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class UpgradeCommand implements ChrisieCommand {

	private BotManagment chrisliebot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Führe Upgrade aus. Bitte prüfe in ein paar Sekunden, ob ich mich irgendwie anders verhalte. Ich hoffe das tut nicht weh ≧☉_☉≦");
		log.info(C.LOG_PUBLIC, "performing upgrade, triggered by {}", m.user().displayName());
		chrisliebot.doShutdown(C.EXIT_CODE_UPGRADE);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
