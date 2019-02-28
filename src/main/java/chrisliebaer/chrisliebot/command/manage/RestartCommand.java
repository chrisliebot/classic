package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RestartCommand implements CommandExecutor {

	private ChrisliebotIrc chrisliebot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Führe Neustart aus...");
		chrisliebot.doShutdown(C.EXIT_CODE_RESTART);
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
