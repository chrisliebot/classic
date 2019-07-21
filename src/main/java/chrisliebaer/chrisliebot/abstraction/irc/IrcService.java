package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.NonNull;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@SuppressWarnings("MethodMayBeSynchronized")
public class IrcService implements ChrislieService {
	
	@Getter private Client client;
	
	private Set<String> admins;
	private Set<String> ignores;
	
	private Consumer<ChrislieMessage> sink;
	
	public IrcService(@NonNull Client client, Set<String> admins, Set<String> ignores) {
		this.client = client;
		this.admins = admins;
		this.ignores = ignores;
		
		client.getEventManager().registerEventListener(this);
	}
	
	@Handler
	public void onChannelMessage(ChannelMessageEvent ev) {
		if (blockedUser(ev.getActor()))
			return;
		
		synchronized (this) {
			if (sink != null)
				sink.accept(IrcMessage.of(this, ev));
		}
	}
	
	@Handler
	public void onPrivateMessage(PrivateMessageEvent ev) {
		if (blockedUser(ev.getActor()))
			return;
		
		synchronized (this) {
			if (sink != null)
				sink.accept(IrcMessage.of(this, ev));
		}
	}
	
	@Override
	public void sink(@Nullable Consumer<ChrislieMessage> sink) {
		synchronized (this) {
			this.sink = sink;
		}
	}
	
	@Override
	public void reconnect() {
		client.reconnect();
	}
	
	@Override
	public void exit() throws Exception {
		client.getEventManager().unregisterEventListener(this);
		client.shutdown();
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
		return user.getAccount().map(admins::contains).orElse(false);
	}
	
	private boolean blockedUser(User user) {
		return client.isUser(user) || ignores.contains(user.getNick());
	}
	
	public static void run(ChrislieService service, Consumer<IrcService> fn) {
		if (service instanceof IrcService)
			fn.accept((IrcService) service);
	}
	
	public static void run(ServiceAttached serviceAttached, Consumer<IrcService> fn) {
		run(serviceAttached.service(), fn);
	}
}
