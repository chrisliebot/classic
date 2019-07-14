package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import lombok.Getter;
import lombok.NonNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;

import java.util.Optional;

public class IrcService implements ChrislieService {
	
	@Getter private Client client;
	
	public IrcService(@NonNull Client client) {
		this.client = client;
	}
	
	@Override
	public Optional<IrcChannel> channel(String identifier) {
		return client.getChannel(identifier)
				.map(channel -> new IrcChannel(this, channel));
	}
	
	@Override
	public Optional<IrcUser> user(String identifier) {
		throw new RuntimeException("not implemented yet");
	}
	
	public boolean isAdmin(User user) {
		throw new RuntimeException("not implemented yet");
	}
}
