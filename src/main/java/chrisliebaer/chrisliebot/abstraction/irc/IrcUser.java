package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.Getter;
import org.kitteh.irc.client.library.element.User;

import java.util.Optional;
import java.util.function.Function;

public class IrcUser implements ChrislieUser {
	
	@Getter private IrcService service;
	@Getter private User user;
	
	private Function<IrcUser, String> idFn;
	
	public IrcUser(IrcService service, User user) {
		this(service, user, IrcUser::createIdFromAccountOrFallback);
	}
	
	private IrcUser(IrcService service, User user, Function<IrcUser, String> idFn) {
		this.service = service;
		this.user = user;
		this.idFn = idFn;
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
		return idFn.apply(this);
	}
	
	@Override
	public String mention() {
		return user.getNick();
	}
	
	@Override
	public Optional<? extends ChrislieChannel> directMessage() {
		return Optional.of(new IrcPrivateChannel(service, user));
	}
	
	/**
	 * @return a new {@link ChrislieUser} with the account indentifier dropped.
	 */
	public IrcUser asNickname() {
		return new IrcUser(service, user, IrcUser::createIdFromNickIgnoreAccount);
	}
	
	private static String createIdFromAccountOrFallback(IrcUser ircUser) {
		var user = ircUser.user;
		return user.getAccount().map(s -> IrcService.PREFIX_USER_BY_ACCOUNT + s)
				.orElse(IrcService.PREFIX_USER_BY_NICKNAME + user.getNick());
	}
	
	private static String createIdFromNickIgnoreAccount(IrcUser ircUser) {
		return IrcService.PREFIX_USER_BY_NICKNAME + ircUser.user.getNick();
	}
}
