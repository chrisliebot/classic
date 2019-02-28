package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JoinCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		String[] args = arg.split(" ", 2);
		
		try {
			if (arg.isEmpty()) {
				m.reply(C.error("Kein Channel angegeben."));
			} else if (args.length == 1) {
				log.info(C.LOG_IRC, "attempting to join {}, triggered by {}", args[0], m.user().getNick());
				m.getClient().addChannel(args[0]);
			} else if (args.length == 2) {
				log.info(C.LOG_IRC, "attempting to join {} with passwort, triggered by {}", args[0], m.user().getNick());
				m.getClient().addKeyProtectedChannel(args[0], args[1]);
			}
		} catch (IllegalArgumentException ignored) {
			log.warn("channel doesn't exist: {}", args[0]);
			m.reply(C.invalidChannel(args[0]));
		}
		
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
