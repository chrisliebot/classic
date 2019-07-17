package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.abstraction.irc.IrcUser;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.element.User;

@Slf4j
public class PartCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		IrcService.run(m, ircService -> {
			var client = ircService.client();
			
			String channelName = null;
			if (!arg.isEmpty())
				channelName = arg;
			else if (!m.channel().isDirectMessage())
				channelName = m.channel().identifier();
			
			if (channelName == null)
				m.reply(C.error("Kein Channel angegeben."));
			else {
				// check if we have even joined channel
				final String finalChannel = channelName;
				client.getChannels().stream().filter(chan -> chan.getName().equals(finalChannel)).findAny().ifPresentOrElse(channel -> {
					
					// check for admin or channel operator
					User user = ((IrcUser)m.user()).user();
					if (m.user().isAdmin() || C.isChannelOp(channel, user)) {
						log.info(C.LOG_PUBLIC, "leaving channel {} on behalf of {}", finalChannel, m.user().displayName());
						channel.part("Auf Anweisung von " + m.user().displayName());
					} else {
						m.reply(C.error("Du bist weder ein Admin noch ein Operator im Zielchannel."));
					}
				}, () -> m.reply("So einen Channel kenne ich nicht."));
			}
		});
	}
}
