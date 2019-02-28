package chrisliebaer.chrisliebot.command.random;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

import java.util.concurrent.ThreadLocalRandom;

public class CoinCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Die Münze zeigt " +
				(ThreadLocalRandom.current().nextBoolean() ?
						C.highlight("Kopf") :
						C.highlight("Zahl")));
	}
}
