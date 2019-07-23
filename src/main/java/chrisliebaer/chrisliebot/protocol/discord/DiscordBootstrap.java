package chrisliebaer.chrisliebot.protocol.discord;

import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.protocol.ServiceBootstrap;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;

public class DiscordBootstrap implements ServiceBootstrap {
	
	private DiscordConfig config;
	
	public DiscordBootstrap(@NonNull DiscordConfig config) {
		this.config = config;
	}
	
	@Override
	@SneakyThrows
	public DiscordService service() {
		var jda = new JDABuilder(config.token())
				.setEventManager(new AnnotatedEventManager())
				.build();
		return new DiscordService(jda, config.admins());
	}
}
