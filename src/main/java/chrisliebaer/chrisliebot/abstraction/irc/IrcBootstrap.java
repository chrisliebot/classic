package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ServiceBootstrap;
import chrisliebaer.chrisliebot.util.ClientLogic;
import chrisliebaer.chrisliebot.util.IrcToSqlLogger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.command.CapabilityRequestCommand;
import org.kitteh.irc.client.library.element.CapabilityState;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesAcknowledgedEvent;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesRejectedEvent;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesSupportedListEvent;
import org.kitteh.irc.client.library.feature.CapabilityManager;
import org.kitteh.irc.client.library.feature.sending.SingleDelaySender;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class IrcBootstrap implements ServiceBootstrap {
	
	private boolean chatlog; // enable logging of all messages to database
	
	private String host;
	private Integer port;
	private String user; // for login, not nickname
	private String nickname;
	private String serverPassword;
	private boolean secure; // enables tls
	private Integer flooding; // delay between messenges in ms
	private String realname;
	
	private Map<String, List<String>> guilds;
	
	private Set<String> ignore;
	
	@Override
	public IrcService service(Chrisliebot bot, String identifier) throws NullPointerException {
		
		var builder = Client.builder();
		builder.listeners().exception(IrcBootstrap::exceptionLogger);
		
		configureConnection(builder);
		
		// protocol level logging
		IrcLogger.attach(identifier, builder);
		
		Client client = builder.build();
		
		// try to enable echo capability so we can react to our own messages
		client.getEventManager().registerEventListener(new EchoCapHandler());
		
		// provide meaningful reaction to some very specific events
		client.getEventManager().registerEventListener(new ClientLogic());
		
		// log chat messages in database
		if (chatlog)
			chatLogger(client, bot);
		
		// connect and pass to service, service should not assume client to be still disconnected, setup is done here
		client.connect();
		return new IrcService(client, identifier, buildGuildMap(guilds), ignore);
	}
	
	private static Multimap<String, Pattern> buildGuildMap(Map<String, List<String>> guilds) {
		if (guilds == null)
			guilds = Map.of();
		
		Multimap<String, Pattern> guildMap = HashMultimap.create();
		for (var e : guilds.entrySet())
			for (String s : e.getValue())
				guildMap.put(e.getKey(), Pattern.compile(s));
		return guildMap;
	}
	
	private void verboseLogging(Client.Builder builder) {
		builder.listeners().input(IrcBootstrap::inLogger);
		builder.listeners().output(IrcBootstrap::outLogger);
	}
	
	private void configureConnection(Client.Builder builder) {
		var server = builder.server();
		
		server.host(host);
		
		if (port != null)
			server.port(port);
		
		if (user != null)
			builder.user(user);
		
		if (nickname != null)
			builder.nick(nickname);
		
		if (serverPassword != null)
			server.password(serverPassword);
		
		if (secure)
			server.secure(true);
		
		if (flooding != null)
			builder.management().messageSendingQueueSupplier(SingleDelaySender.getSupplier(flooding));
		
		if (realname != null)
			builder.realName(realname);
	}
	
	private void chatLogger(Client client, Chrisliebot bot) {
		client.getEventManager().registerEventListener(new IrcToSqlLogger(bot.sharedResources().dataSource()));
	}
	
	private static void inLogger(String line) {
		log.trace("<<< {}", line);
	}
	
	private static void outLogger(String line) {
		log.trace(">>> {}", line);
	}
	
	private static void exceptionLogger(Throwable t) {
		log.error("error in irc library", t);
	}
	
	/**
	 * Adding this handler to the event processor will enable the "echo-message" capability if supported by the server.
	 */
	private static class EchoCapHandler {
		
		@Handler
		public void capList(CapabilitiesSupportedListEvent ev) {
			if (ev.isNegotiating()) {
				Optional<CapabilityState> opt = ev.getSupportedCapabilities()
						.stream().filter(s -> s.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE)).findFirst();
				
				if (opt.isEmpty()) {
					log.warn("echo-message is not supported by server, will not be able to log own messages");
					return;
				}
				
				// request echo cap
				new CapabilityRequestCommand(ev.getClient()).enable(CapabilityManager.Defaults.ECHO_MESSAGE).execute();
			}
		}
		
		@Handler
		public void capAck(CapabilitiesAcknowledgedEvent ev) {
			if (ev.getAcknowledgedCapabilities().stream().anyMatch(c -> c.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE))) {
				log.info("enabled echo-message capability");
			}
		}
		
		@Handler
		public void capAck(CapabilitiesRejectedEvent ev) {
			if (ev.getRejectedCapabilitiesRequest().stream().anyMatch(c -> c.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE))) {
				log.error("failed to enable echo-message, will not be able to react to own messages (including logging)");
			}
		}
	}
	
}
