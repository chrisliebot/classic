package chrisliebaer.chrisliebot.command.choice;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ChoiceCommand implements CommandExecutor {
	
	private static final String[] FLAVOUR = {
			"Ganz klar",
			"Ich bin für",
			"In jedem Fall",
			"Definitiv",
			"Zu 100%",
			"Wie wärs mit"
	};
	
	@Override
	public void execute(Message m, String arg) {
		var choices = Arrays.stream(arg.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
		if (choices.isEmpty()) {
			m.reply(C.error("Keine Auswahloptionen gefunden."));
			return;
		}
		
		var tlr = ThreadLocalRandom.current();
		var choice = choices.get(tlr.nextInt(choices.size()));
		var flavour = FLAVOUR[tlr.nextInt(FLAVOUR.length)];
		m.reply(flavour + " " + C.highlight(choice));
	}
}
