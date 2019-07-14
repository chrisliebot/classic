package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.PrivateMessage;
import lombok.Getter;
import lombok.NonNull;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

public class IrcMessage implements ChrislieMessage {
	
	@Getter private IrcService service;
	@Getter private IrcUser user;
	@Getter private ChrislieChannel channel; // we have no irc abstraction that combines channels and private messages
	@Getter private String message;
	
	public IrcMessage(@NonNull IrcService service,
					  @NonNull IrcUser user,
					  @NonNull ChrislieChannel channel,
					  @NonNull String message) {
		this.service = service;
		this.user = user;
		this.channel = channel;
		this.message = message;
	}
	
	public static IrcMessage of(IrcService service, PrivateMessage ev) {
		return new IrcMessage(service,
				new IrcUser(service, ev.user()),
				new IrcPrivateChannel(service, ev.user()),
				ev.message());
	}
	
	public static IrcMessage of(IrcService service, ChannelMessageEvent ev) {
		return new IrcMessage(service,
				new IrcUser(service, ev.getActor()),
				new IrcChannel(service, ev.getChannel()),
				ev.getMessage());
	}
}
