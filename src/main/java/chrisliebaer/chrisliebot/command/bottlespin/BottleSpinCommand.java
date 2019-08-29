package chrisliebaer.chrisliebot.command.bottlespin;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class BottleSpinCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Dreh die Flasche und finde den Nutzer auf den sie zeigt.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var rng = ThreadLocalRandom.current();
		
		var m = invc.msg();
		var reply = invc.reply()
				.title("Die Flasche hat entschieden");
		
		if (m.channel().isDirectMessage()) {
			var choice = rng.nextBoolean() ? "mich" : "dich";
			
			reply.description(out -> out.appendEscape(choice))
					.replace()
					.appendEscape("Die Flasche hat ")
					.appendEscape(choice, ChrislieFormat.HIGHLIGHT)
					.appendEscape(" ausgewählt.");
		} else {
			var userList = m.channel().users();
			var user = userList.get(rng.nextInt(userList.size()));
			
			reply.description(out -> out.append(user.mention()))
					.replace()
					.appendEscape("Die Flasche hat ")
					.appendEscape(user.displayName(), ChrislieFormat.HIGHLIGHT)
					.appendEscape(" ausgewählt.");
		}
		reply.send();
	}
}
