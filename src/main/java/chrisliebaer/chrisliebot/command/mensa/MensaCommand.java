package chrisliebaer.chrisliebot.command.mensa;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

public class MensaCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Benutz einfach -mensa oder #!mensa. Bis das hier fertig ist wirds noch eine Weile dauern.");
	}
}
