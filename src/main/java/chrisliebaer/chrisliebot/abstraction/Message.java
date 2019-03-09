package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.config.ConfigContext;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ClientLinked;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import javax.annotation.Nullable;
import java.util.Optional;

public interface Message extends ClientLinked {
	
	public User user();
	
	public String message();
	
	public void reply(@Nullable String s);
	
	public Optional<Channel> channel();
	
	@Override
	public default Client getClient() {
		return user().getClient();
	}
	
	public default String source() {
		return channel().map(Channel::getName).orElse(user().getNick());
	}
	
	public boolean isAdmin();
	
	public static Message of(PrivateMessageEvent ev, ConfigContext ctx) {
		return new PrivateMessage(ev, ctx);
	}
	
	public static Message of(ChannelMessageEvent ev, ConfigContext ctx) {
		return new ChannelMessage(ev, ctx);
	}
}
