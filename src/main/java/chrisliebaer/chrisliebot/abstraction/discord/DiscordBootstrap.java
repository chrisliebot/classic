package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ServiceBootstrap;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;

import javax.security.auth.login.LoginException;

public class DiscordBootstrap implements ServiceBootstrap {
	
	private String token;
	
	@Override
	public DiscordService service(Chrisliebot bot, String identifier) throws LoginException {
		var jda = new JDABuilder(token)
				.setEventManager(new AnnotatedEventManager())
				.build();
		return new DiscordService(jda, identifier);
	}
}
