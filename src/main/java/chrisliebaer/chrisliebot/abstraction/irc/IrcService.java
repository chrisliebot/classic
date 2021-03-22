package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class IrcService implements ChrislieService {
	
	/* IRC has a problem: it doesn't share a lot of information about clients without being asked. This means we have to get creative.
	 * so first of all, we don't have a per user identifier, a user might just use a nickname so we introduce a few limitation on the reliability of this service
	 *
	 * 1. identifiers contain information about how they relate to a user, this allows us to lock account identifier to an account
	 * 2. we use nick identifiers that are more or less a best effort attempt of identifying a user
	 * 3. we can only instance ChrislieUser objects for users that we have access to, this means users have to share a channel with us, otherwise they don't exist
	 * 4. While channels are relatively easy in IRC, we use the same prefix system for creating channel identifiers in private conversations
	 *
	 * This identifier scheme is an implementation detail and may change any time, code outside of this service should therefore not make any assumptions
	 * about it's existance.
	 */
	public static final String PREFIX_USER_BY_ACCOUNT = "ACCOUNT:";
	
	public static final String PREFIX_USER_BY_NICKNAME = "NICK:";
	
	@Getter private final Client client;
	@Getter private final String identifier;
	private final Multimap<String, Pattern> guildMap;
	private Set<String> ignore;
	
	@Setter private Consumer<ChrislieMessage> sink;
	
	public IrcService(@NonNull Client client, @NonNull String identifier, Multimap<String, Pattern> guildMap, Set<String> ignore) {
		this.client = client;
		this.identifier = identifier;
		this.guildMap = guildMap;
		this.ignore = ignore;
	}
	
	protected Optional<IrcGuild> channelToGuild(Channel channel) {
		var channelName = channel.getName();
		for (var e : guildMap.asMap().entrySet()) {
			var name = e.getKey();
			for (var pattern : e.getValue()) {
				if (pattern.matcher(channelName).find())
					return guild(name);
			}
		}
		return Optional.empty();
	}
	
	@Override
	public void awaitReady() throws Exception {
		client.getEventManager().registerEventListener(this);
		
		// TODO: check if and how we can ensure we are somewhat ready for connections
	}
	
	@Handler
	public void onChannelMessage(ChannelMessageEvent ev) {
		if (ignore.contains(ev.getActor().getNick()) || client.isUser(ev.getActor()))
			return;
		
		var sink = this.sink;
		if (sink != null)
			sink.accept(IrcMessage.of(this, ev));
	}
	
	@Handler
	public void onPrivateMessage(PrivateMessageEvent ev) {
		if (ignore.contains(ev.getActor().getNick()) || client.isUser(ev.getActor()))
			return;
		
		var sink = this.sink;
		if (sink != null)
			sink.accept(IrcMessage.of(this, ev));
	}
	
	@Handler
	public void onDisconnect(ClientConnectionEndedEvent ev) {
		if (ev.canAttemptReconnect()) {
			ev.getCause().ifPresentOrElse(
					e -> log.info("service {} lost connection, attempting reconnect", identifier, e),
					() -> log.info("service {} lost connection witout cause", identifier)
			);
			
			ev.setAttemptReconnect(true);
		}
	}
	
	@Override
	public void reconnect() {
		client.reconnect();
	}
	
	@Override
	public void exit() {
		// TODO: actually wait for full shutdown
		client.getEventManager().unregisterEventListener(this);
		client.shutdown();
	}
	
	@Override
	public Optional<? extends ChrislieChannel> channel(String identifier) {
		
		var prefixes = client.getServerInfo().getChannelPrefixes();
		var isChannel = prefixes.stream()
				.anyMatch(p -> identifier.startsWith(String.valueOf(p)));
		
		if (isChannel) {
			return client.getChannel(identifier)
					.map(channel -> {
						var guildIdentifier = channelToGuild(channel);
						return new IrcChannel(this, channel, guildIdentifier.orElse(null));
					});
		} else {
			return userByPrefixedIdentifier(identifier).map(user -> new IrcPrivateChannel(this, user));
		}
	}
	
	@Override
	public Optional<IrcUser> user(String identifier) {
		return userByPrefixedIdentifier(identifier).map(user -> new IrcUser(this, user));
	}
	
	protected Optional<User> userByPrefixedIdentifier(String prefixedIdentifier) {
		// creates a stream of all users known to this instance
		var userStream = client.getChannels().stream()
				.map(Channel::getUsers)
				.flatMap(Collection::stream);
		return userByPrefixedIdentifier(prefixedIdentifier, userStream);
	}
	
	// This method resolves a prefixed indentifier, as it is used by the irc service to a library user instance.
	protected Optional<User> userByPrefixedIdentifier(String prefixedIdentifier, Stream<User> userStream) {
		Predicate<User> pred;
		if (prefixedIdentifier.startsWith(PREFIX_USER_BY_ACCOUNT)) {
			pred = userByAccount(prefixedIdentifier.substring(PREFIX_USER_BY_ACCOUNT.length()));
		} else if (prefixedIdentifier.startsWith(PREFIX_USER_BY_NICKNAME)) {
			pred = userByNickname(prefixedIdentifier.substring(PREFIX_USER_BY_NICKNAME.length()));
		} else {
			throw new IllegalArgumentException("unkown prefix in user identifier: " + prefixedIdentifier);
		}
		
		return findUser(pred, userStream);
	}
	
	public Optional<User> findUser(Predicate<User> predicate, Stream<User> userStream) {
		return userStream
				.filter(predicate)
				.findFirst();
	}
	
	private static Predicate<User> userByAccount(String account) {
		return user -> user.getAccount().filter(s -> s.equals(account)).isPresent();
	}
	
	private static Predicate<User> userByNickname(String nickname) {
		return user -> user.getNick().equals(nickname);
	}
	
	@Override
	public Optional<IrcGuild> guild(String identifier) {
		
		var patterns = guildMap.get(identifier);
		if (patterns == null)
			return Optional.empty();
		
		var pred = patterns.stream()
				.map(Pattern::asPredicate)
				.reduce(Predicate::or).orElse(s -> false); // if no regex is given, we still instance the guild with no channels
		
		var channels = client.getChannels().stream()
				.filter(channel -> pred.test(channel.getName()))
				.collect(Collectors.toList());
		
		return Optional.of(new IrcGuild(this, identifier, channels));
	}
	
	public static boolean isIrc(ServiceAttached service) {
		return service.service() instanceof IrcService;
	}
}
