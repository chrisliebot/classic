package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import com.google.common.base.Objects;
import lombok.Getter;
import org.kitteh.irc.client.library.element.Channel;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IrcChannel implements ChrislieChannel {
	
	@Getter private IrcService service;
	@Getter private Channel channel;
	private IrcGuild guild;
	
	public IrcChannel(IrcService service, Channel channel, IrcGuild guild) {
		this.service = service;
		this.channel = channel;
		this.guild = guild;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IrcChannel that = (IrcChannel) o;
		return Objects.equal(channel, that.channel);
	}
	
	@Override
	public int hashCode() {
		return channel.hashCode();
	}
	
	@Override
	public String displayName() {
		return channel.getName();
	}
	
	@Override
	public String identifier() {
		return channel.getName();
	}
	
	@Override
	public boolean isDirectMessage() {
		return false;
	}
	
	@Override
	public List<IrcUser> users() {
		return channel.getUsers().stream()
				.map(user -> new IrcUser(service, user))
				.collect(Collectors.toList());
	}
	
	@Override
	public Optional<IrcUser> user(String identifier) {
		return service.userByPrefixedIdentifier(identifier, channel.getUsers().stream()).map(user -> new IrcUser(service, user));
	}
	
	@Override
	public Optional<IrcUser> resolve(String callName) {
		return channel.getUser(callName)
				.map(user -> new IrcUser(service, user))
				.or(() -> user(callName));
	}
	
	@Override
	public Optional<IrcGuild> guild() {
		return Optional.ofNullable(guild);
	}
	
	@Override
	public IrcOutput output(LimiterConfig limiterConfig) {
		return new IrcOutput(this::escapeNicks, s -> limiterConfig.send(channel, s));
	}
	
	/**
	 * Takes a string and escapes it in such a way that it will not mention any users in this channel.
	 *
	 * @param s The string to escape.
	 * @return The escaped string.
	 */
	public String escapeNicks(String s) {
		Pattern nickPattern = Pattern.compile(
				channel.getNicknames().stream().map(Pattern::quote).collect(Collectors.joining("|")),
				Pattern.CASE_INSENSITIVE);
		return nickPattern.matcher(s).replaceAll(result -> {
			var nickname = result.group();
			if (nickname.length() <= 1)
				return nickname;
			
			return nickname.substring(0, 1) + C.ZERO_WIDTH_NO_BREAK_SPACE + nickname.substring(1);
		});
	}
}
