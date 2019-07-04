package chrisliebaer.chrisliebot.command.mock;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

import java.util.concurrent.ThreadLocalRandom;

public class MockCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		
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
		
		m.reply(new String(chars));
	}
}
