package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.NonNull;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;

import java.util.Optional;
import java.util.function.Consumer;

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
		// TODO: implement me
		return false;
	}
	
	public boolean ignore(String nickname) {
		// TODO: implement me
		return false;
	}
	
	public static void run(ChrislieService service, Consumer<IrcService> fn) {
		if (service instanceof IrcService)
			fn.accept((IrcService) service);
	}
	
	public static void run(ServiceAttached serviceAttached, Consumer<IrcService> fn) {
		run(serviceAttached.service(), fn);
	}
}
