package chrisliebaer.chrisliebot.command.mock;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class MockCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Wenn eine Aussage so dumm ist, dass es keinen weiteren Kommentar bedarf.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var arg = invc.arg();
		if (arg.isBlank())
			return;
		
		var chars = arg.toCharArray();
		var state = ThreadLocalRandom.current().nextBoolean();
		for (int i = 0; i < chars.length; i++) {
			if (state)
				chars[i] = Character.toLowerCase(chars[i]);
			else
				chars[i] = Character.toUpperCase(chars[i]);
			state = !state;
		}
		
		invc.reply(new String(chars));
	}
}
