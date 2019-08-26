package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import java.util.concurrent.ThreadLocalRandom;

public class CoinCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply()
				.title("Die Münze ist gefallen")
				.description(out -> {
					out.appendEscape(ThreadLocalRandom.current().nextBoolean() ? "Kopf" : "Zahl", ChrislieFormat.HIGHLIGHT);
				})
				.send();
	}
}
