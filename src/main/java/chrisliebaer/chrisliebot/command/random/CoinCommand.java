package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class CoinCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Wirf eine Münze und lass den Zufall für dich entscheiden. Sei faul!");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		invc.reply()
				.title("Die Münze ist gefallen")
				.description(out -> out.appendEscape(ThreadLocalRandom.current().nextBoolean() ? "Kopf" : "Zahl", ChrislieFormat.HIGHLIGHT))
				.send();
	}
}
