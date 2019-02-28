package chrisliebaer.chrisliebot.command.bottlespin;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import org.kitteh.irc.client.library.element.Channel;

import java.util.concurrent.ThreadLocalRandom;

public class BottleSpinCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		var rng = ThreadLocalRandom.current();
		if (m.channel().isPresent()) {
			Channel chan = m.channel().get();
			var userList = chan.getUsers();
			var user = userList.get(rng.nextInt(userList.size()));
			m.reply("Die Flasche hat " + C.highlight(user.getNick()) + " ausgewählt.");
		} else {
			boolean me = rng.nextBoolean();
			m.reply("Die Flasche hat " +
					(me ? C.highlight("mich") : C.highlight("dich")) +
					" ausgewählt.");
		}
	}
}
