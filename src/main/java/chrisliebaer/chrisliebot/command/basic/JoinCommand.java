package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.Client;

@Slf4j
public class JoinCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		IrcService.run(m, ircService -> {
			String[] args = arg.split(" ", 2);
			
			Client client = ircService.client();
			
			try {
				if (arg.isEmpty()) {
					m.reply(C.error("Kein Channel angegeben."));
				} else if (args.length == 1) {
					log.info(C.LOG_PUBLIC, "attempting to join {}, triggered by {}", args[0], m.user().displayName());
					client.addChannel(args[0]);
				} else if (args.length == 2) {
					log.info(C.LOG_PUBLIC, "attempting to join {} with passwort, triggered by {}", args[0], m.user().displayName());
					client.addKeyProtectedChannel(args[0], args[1]);
				}
			} catch (IllegalArgumentException ignored) {
				log.warn("channel doesn't exist: {}", args[0]);
				m.reply(C.invalidChannel(args[0]));
			}
		});
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
