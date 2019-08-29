package chrisliebaer.chrisliebot.abstraction.irc;


import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.Getter;
import org.kitteh.irc.client.library.element.User;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class IrcPrivateChannel implements ChrislieChannel {
	
	
	@Getter private IrcService service;
	@Getter private IrcUser user;
	
	public IrcPrivateChannel(IrcService service, User user) {
		this.service = service;
		this.user = new IrcUser(service, user);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IrcPrivateChannel that = (IrcPrivateChannel) o;
		return user.equals(that.user);
	}
	
	@Override
	public int hashCode() {
		return user.hashCode();
	}
	
	@Override
	public String displayName() {
		return user.displayName();
	}
	
	@Override
	public String identifier() {
		return user.identifier();
	}
	
	@Override
	public boolean isDirectMessage() {
		return true;
	}
	
	@Override
	public List<IrcUser> users() {
		return List.of(user);
	}
	
	@Override
	public Optional<IrcUser> user(String identifier) {
		if (user.identifier().equals(identifier))
			return Optional.of(user);
		return Optional.empty();
	}
	
	@Override
	public Optional<IrcUser> resolve(String callName) {
		var user = this.user.user();
		if (user.getNick().equalsIgnoreCase(callName))
			return Optional.of(this.user);
		return Optional.empty();
	}
	
	@Override
	public Optional<IrcGuild> guild() {
		return Optional.empty();
	}
	
	@Override
	public IrcOutput output(LimiterConfig limiterConfig) {
		return new IrcOutput(Function.identity(), s -> limiterConfig.send(user.user(), s));
	}
}
