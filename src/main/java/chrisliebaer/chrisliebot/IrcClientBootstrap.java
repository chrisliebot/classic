package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.config.ChrislieConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.feature.sending.SingleDelaySender;

@Slf4j
public class IrcClientBootstrap {
	
	private ChrislieConfig.ConnectionConfig config;
	private Client.Builder builder;
	
	public IrcClientBootstrap(@NonNull ChrislieConfig.ConnectionConfig config) {
		this.config = config;
		
		builder = Client.builder();
		configureBuilder();
	}
	
	public void verboseLogging() {
		builder.listeners().input(this::inLogger);
		builder.listeners().output(this::outLogger);
	}
	
	private void inLogger(String line) {
		log.trace("<<< {}", line);
	}
	
	private void outLogger(String line) {
		log.trace(">>> {}", line);
	}
	
	
	private void configureBuilder() {
		var server = builder.server();
		
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
	
	public Client.Builder getBuilder() {
		return builder;
	}
}
