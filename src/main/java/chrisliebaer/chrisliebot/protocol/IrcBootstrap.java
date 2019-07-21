package chrisliebaer.chrisliebot.protocol;

import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.protocol.irc.EchoCapHandler;
import chrisliebaer.chrisliebot.protocol.irc.IrcConfig.BotConfig;
import chrisliebaer.chrisliebot.util.ChrislieCutter;
import chrisliebaer.chrisliebot.util.ClientLogic;
import chrisliebaer.chrisliebot.util.IrcLogAppender;
import chrisliebaer.chrisliebot.util.IrcToSqlLogger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.sending.SingleDelaySender;

@Slf4j
public class IrcBootstrap implements ServiceBootstrap {
	
	private BotConfig config;
	
	public IrcBootstrap(@NonNull BotConfig config) {
		this.config = config;
	}
	
	@Override
	public IrcService service() {
		var builder = Client.builder();
		builder.listeners().exception(IrcBootstrap::exceptionLogger);
		
		configureConnection(builder);
		
		// protocol level logging
		if (config.verbose())
			verboseLogging(builder);
		
		Client client = builder.build();
		
		// built-in cutter is not working properly
		client.setMessageCutter(new ChrislieCutter());
		
		// try to enable echo capability so we can react to our own messages
		client.getEventManager().registerEventListener(new EchoCapHandler());
		
		// provide meaningful reaction to some very specific events
		client.getEventManager().registerEventListener(new ClientLogic());
		
		// public logging of status messages
		if (config.logTarget() != null && !config.logTarget().isBlank())
			logAppender(client);
		
		// log chat messages in database
		if (config.chatlog())
			chatLogger(client);
		
		// connect and pass to service, service should not assume client to be still disconnected, setup is done here
		client.connect();
		return new IrcService(client, config.admins(), config.ignore());
	}
	
	private void verboseLogging(Client.Builder builder) {
		builder.listeners().input(IrcBootstrap::inLogger);
		builder.listeners().output(IrcBootstrap::outLogger);
	}
	
	private void logAppender(Client client) {
		LoggerConfig rootCfg = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getRootLogger();
		var appender = new IrcLogAppender(client, config.logTarget());
		appender.start();
		rootCfg.addAppender(appender, Level.ALL, null);
	}
	
	private void configureConnection(Client.Builder builder) {
		var server = builder.server();
		var config = this.config.connection();
		
		server.host(config.host());
		
		if (config.port() != null)
			server.port(config.port());
		
		if (config.user() != null)
			builder.user(config.user());
		
		if (config.nickname() != null)
			builder.nick(config.nickname());
		
		if (config.serverPassword() != null)
			server.password(config.serverPassword());
		
		if (config.secure() != null)
			server.secure(config.secure());
		
		if (config.flooding() != null)
			builder.management().messageSendingQueueSupplier(SingleDelaySender.getSupplier(config.flooding()));
		
		if (config.realname() != null)
			builder.realName(config.realname());
	}
	
	private void chatLogger(Client client) {
		client.getEventManager().registerEventListener(new IrcToSqlLogger());
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
}
