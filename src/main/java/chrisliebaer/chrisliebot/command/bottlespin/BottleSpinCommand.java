package chrisliebaer.chrisliebot.command.bottlespin;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import java.util.concurrent.ThreadLocalRandom;

public class BottleSpinCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		var rng = ThreadLocalRandom.current();
		if (m.channel().isDirectMessage()) {
			boolean me = rng.nextBoolean();
			m.reply("Die Flasche hat " +
					(me ? C.highlight("mich") : C.highlight("dich")) +
					" ausgewählt.");
		} else {
			ChrislieChannel chan = m.channel();
			var userList = chan.users();
			var user = userList.get(rng.nextInt(userList.size()));
			m.reply("Die Flasche hat " + C.highlight(user.displayName()) + " ausgewählt.");
		}
	}
}
