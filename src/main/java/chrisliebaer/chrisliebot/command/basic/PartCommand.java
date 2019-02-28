package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PartCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		var client = m.getClient();
		
		String channelName = null;
		if (!arg.isEmpty())
			channelName = arg;
		else if (m.channel().isPresent())
			channelName = m.channel().get().getName();
		
		if (channelName == null)
			m.reply(C.error("Kein Channel angegeben."));
		else {
			// check if we have even joined channel
			final String finalChannel = channelName;
			client.getChannels().stream().filter(chan -> chan.getName().equals(finalChannel)).findAny().ifPresentOrElse(channel -> {
				
				// check for admin or channel operator
				if (m.isAdmin() || C.isChannelOp(channel, m.user())) {
					log.info(C.LOG_IRC, "leaving channel {} on behalf of {}", finalChannel, m.user().getNick());
					channel.part("Auf Anweisung von " + m.user().getNick());
				} else {
					m.reply(C.error("Du bist weder ein Admin noch ein Operator im Zielchannel."));
				}
			}, () -> m.reply("So einen Channel kenne ich nicht."));
		}
	}
}
