package chrisliebaer.chrisliebot.abstraction.irc;


import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.Getter;
import org.kitteh.irc.client.library.element.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class IrcPrivateChannel implements ChrislieChannel {
	
	@Getter private IrcService service;
	@Getter private User user;
	
	public IrcPrivateChannel(IrcService service, User user) {
		this.service = service;
		this.user = user;
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
		return Objects.hash(user);
	}
	
	@Override
	public String displayName() {
		return user.getNick();
	}
	
	@Override
	public String identifier() {
		return user.getNick();
	}
	
	@Override
	public boolean isDirectMessage() {
		return true;
	}
	
	@Override
	public List<IrcUser> users() {
		return List.of(new IrcUser(service, user));
	}
	
	@Override
	public Optional<IrcUser> user(String identifier) {
		throw new RuntimeException("not yet implemented");
	}
	
	@Override
	public Optional<IrcUser> resolve(String callName) {
		throw new RuntimeException("not yet implemented");
	}
	
	@Override
	public IrcOutput output(LimiterConfig limiterConfig) {
		return new IrcOutput(Function.identity(), s -> limiterConfig.send(user, s));
	}
}
