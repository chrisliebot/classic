package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import java.util.concurrent.ThreadLocalRandom;

public class CoinCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Die Münze zeigt " +
				(ThreadLocalRandom.current().nextBoolean() ?
						C.highlight("Kopf") :
						C.highlight("Zahl")));
	}
}
