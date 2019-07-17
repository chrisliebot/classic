package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import org.kitteh.irc.client.library.element.Channel;

import java.util.stream.Collectors;

public class ChannelListCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		IrcService.run(m, ircService -> {
			var chanList = ircService.client().getChannels().stream()
					.map(Channel::getName)
					.map(C::highlight)
					.collect(Collectors.joining(", "));
			m.reply("Ich bin Mitglied der folgenden Channel: " + chanList);
		});
	}
}
