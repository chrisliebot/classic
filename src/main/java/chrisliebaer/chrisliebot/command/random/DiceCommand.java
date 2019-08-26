package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;

import java.util.concurrent.ThreadLocalRandom;

public class DiceCommand implements ChrisieCommand {
	
	private long defNumber;
	
	public DiceCommand(long defNumber) {
		this.defNumber = defNumber;
	}
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		try {
			long max = defNumber;
			
			if (!arg.isEmpty())
				max = Long.parseLong(arg);
			
			if (max < 1) {
				long finalMax = max;
				ErrorOutputBuilder
						.generic(out -> out.appendEscape(String.valueOf(finalMax), ChrislieFormat.HIGHLIGHT).appendEscape(" ist zu klein."))
						.write(m);
				return;
			}
			long n = ThreadLocalRandom.current().nextLong(1, max + 1);
			m.reply()
					.title("Die Würfel sind gefallen")
					.description(out -> out.appendEscape(String.valueOf(n), ChrislieFormat.HIGHLIGHT))
					.send();
		} catch (NumberFormatException e) {
			ErrorOutputBuilder.generic("Diese Zahl kenn ich nicht.").write(m);
		}
	}
}
