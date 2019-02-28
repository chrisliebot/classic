package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import org.kitteh.irc.client.library.element.Channel;

import java.util.stream.Collectors;

public class ChannelListCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		var chanList = m.getClient().getChannels().stream()
				.map(Channel::getName)
				.map(C::highlight)
				.collect(Collectors.joining(", "));
		m.reply("Ich bin Mitglied der folgenden Channel: " + chanList);
	}
}
