package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class DiceCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Du hast den Würfel für den D&D Abend vergessen oder willst eine Klausuraufgabe lösen? Dann bist du hier genau richtig.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		try {
			var arg = invc.arg();
			long max;
			if (!arg.isEmpty())
				max = Long.parseLong(arg);
			else
				max = invc.ref().flexConf().getLongOrFail("dice.max");
			
			if (max < 1) {
				ErrorOutputBuilder
						.generic(out -> out.appendEscape(String.valueOf(max), ChrislieFormat.HIGHLIGHT).appendEscape(" ist zu klein."))
						.write(invc).send();
				return;
			}
			long n = ThreadLocalRandom.current().nextLong(1, max + 1);
			invc.reply()
					.title("Die Würfel sind gefallen")
					.description(out -> out.appendEscape(String.valueOf(n), ChrislieFormat.HIGHLIGHT))
					.send();
		} catch (NumberFormatException e) {
			ErrorOutputBuilder.generic("Diese Zahl kenn ich nicht.").write(invc).send();
		}
	}
}
