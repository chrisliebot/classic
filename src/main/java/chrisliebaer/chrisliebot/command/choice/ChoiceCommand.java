package chrisliebaer.chrisliebot.command.choice;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ChoiceCommand implements ChrislieListener.Command {
	
	private static final String[] FLAVOUR = {
			"Ganz klar",
			"Ich bin für",
			"In jedem Fall",
			"Definitiv",
			"Zu 100%",
			"Wie wärs mit",
			"Wenn du mich fragst",
			"Mit Sicherheit",
			"Natürlich",
			"Auf keinen Fall"
	};
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Ich helf dir beim Treffen von wichtigen Entscheidungen: `option1, option2, ...`");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var arg = invc.arg();
		var choices = Arrays.stream(arg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
		if (choices.isEmpty()) {
			ErrorOutputBuilder.generic("Keine Auswahloptionen gefunden.").write(invc).send();
			return;
		}
		
		var tlr = ThreadLocalRandom.current();
		var choice = choices.get(tlr.nextInt(choices.size()));
		var flavour = FLAVOUR[tlr.nextInt(FLAVOUR.length)];
		invc.reply()
				.title("Meine Entscheidung")
				.description(out ->
						out.appendEscape(flavour).appendEscape(" ").appendEscape(choice, ChrislieFormat.HIGHLIGHT))
				.convert("${description}")
				.send();
	}
}
