package chrisliebaer.chrisliebot.command.debug;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DebugCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		if (arg.isEmpty()) {
			m.reply(C.error("Nope"));
			return;
		}
		
		switch (arg) {
			case "spam": {
				m.reply("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
			}
			break;
			case "spaced": {
				m.reply(IntStream.range(0, 500).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
				break;
			}
			case "blob": {
				m.reply(IntStream.range(0, 500).mapToObj(String::valueOf).collect(Collectors.joining("")));
				break;
			}
		}
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
