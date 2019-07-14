package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.abstraction.irc.*;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;

import java.util.function.Function;

@SuppressWarnings("StaticVariableUsedBeforeInitialization")
public final class MigrationHelper {
	
	public static IrcService ircService;
	
	private MigrationHelper() {}
	
	public static ChrislieMessage of(Message m) {
		var user = new IrcUser(ircService, m.user());
		var channel = m.channel().map((Function<Channel, ChrislieChannel>) c -> new IrcChannel(ircService, c))
				.orElse(new IrcPrivateChannel(ircService, m.user()));
		return new IrcMessage(ircService, user, channel, m.message());
	}
	
	public static void setService(Client client) {
		ircService = new IrcService(client);
	}
	
}
