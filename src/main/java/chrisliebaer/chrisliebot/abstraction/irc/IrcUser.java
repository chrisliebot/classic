package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.Getter;
import org.kitteh.irc.client.library.element.User;

import java.util.Optional;

public class IrcUser implements ChrislieUser {
	
	@Getter private IrcService service;
	@Getter private User user;
	
	public IrcUser(IrcService service, User user) {
		this.service = service;
		this.user = user;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IrcUser ircUser = (IrcUser) o;
		return user.equals(ircUser.user);
	}
	
	@Override
	public int hashCode() {
		return user.hashCode();
	}
	
	@Override
	public String displayName() {
		return user.getNick();
	}
	
	@Override
	public String identifier() {
		// we try to lock the itendifier to the nickserv account but fall back to nickname if not available
		return user.getAccount().map(s -> IrcService.PREFIX_USER_BY_ACCOUNT + s)
				.orElse(IrcService.PREFIX_USER_BY_NICKNAME + user.getNick());
	}
	
	@Override
	public String mention() {
		return user.getNick();
	}
	
	@Override
	public Optional<? extends ChrislieChannel> directMessage() {
		return Optional.of(new IrcPrivateChannel(service, user));
	}
}
