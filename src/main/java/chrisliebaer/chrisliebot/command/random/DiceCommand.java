package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

import java.util.concurrent.ThreadLocalRandom;

public class DiceCommand implements CommandExecutor {
	
	private long defNumber;
	
	public DiceCommand(long defNumber) {
		this.defNumber = defNumber;
	}
	
	@Override
	public void execute(Message m, String arg) {
		try {
			long max = defNumber;
			
			if (!arg.isEmpty())
				max = Long.parseLong(arg);
			
			if (max < 1) {
				m.reply(C.error(C.highlight(max) + " ist zu klein."));
				return;
			}
			long n = ThreadLocalRandom.current().nextLong(1, max + 1);
			m.reply("Der Würfel hat entschieden: " + C.highlight(n));
		} catch (NumberFormatException e) {
			m.reply(C.error(C.highlight("'" + arg + "'") + " ist keine Zahl."));
		}
	}
}
