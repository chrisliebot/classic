package chrisliebaer.chrisliebot.command.remote;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class RemoteMessageDto {
	
	// user specific
	private String nickname;
	private String realName;
	private String hostname;
	private String account; // may be null if user is not logged in
	private boolean isAdmin; // user is bot admin
	private Set<Character> modes; // channel modes (like +@)
	
	// message specific
	private String channel; // may be null if private message
	private String message; // full message including command name and prefix
	private String argument; // extracted argument of invocation, may be null if listener invocation
	
	
	public static RemoteMessageDto of(@NonNull ChrislieMessage m) {
		return of(m, null);
	}
	
	public static RemoteMessageDto of(@NonNull ChrislieMessage m, String arg) {
		var user = m.user();
		
		return RemoteMessageDto.builder()
				.nickname(user.softIdentifer())
				/* TODO
				.realName(user.getRealName().orElse(null))
				.hostname(user.getHost())
				.account(user.getAccount().orElse(null))
				.isAdmin(m.isAdmin())
				.modes(getUserModeSet(m.channel().orElse(null), user))
				
				.channel(m.channel().map(Channel::getName).orElse(null))
				*/
				.message(m.message())
				.argument(arg)
				.build();
	}
	
	private static Set<Character> getUserModeSet(Channel channel, User user) {
		if (channel == null)
			return Set.of();
		
		return channel.getUserModes(user)
				.map(channelUserModes -> channelUserModes.stream()
						.map(ChannelUserMode::getNickPrefix).collect(Collectors.toSet()))
				.orElse(Set.of());
	}
}
