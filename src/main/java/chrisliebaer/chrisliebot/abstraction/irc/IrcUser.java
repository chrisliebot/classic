package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.Getter;
import org.kitteh.irc.client.library.element.User;

import java.util.Objects;
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
		return Objects.hash(user);
	}
	
	@Override
	public String displayName() {
		return user.getNick();
	}
	
	@Override
	public Optional<String> identifier() {
		return user.getAccount();
	}
	
	@Override
	public String softIdentifer() {
		return identifier().orElse(user.getNick());
	}
	
	@Override
	public String mention() {
		return user.getNick();
	}
	
	@Override
	public boolean isAdmin() {
		return service.isAdmin(user);
	}
	
	@Override
	public IrcPrivateChannel directMessage() {
		return new IrcPrivateChannel(service, user);
	}
}
