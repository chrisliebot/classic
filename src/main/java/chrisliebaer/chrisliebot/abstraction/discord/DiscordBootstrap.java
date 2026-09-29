package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ServiceBootstrap;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordBootstrap implements ServiceBootstrap {
	
	private String token;
	private boolean updateSlashCommands;
	
	@Override
	public DiscordService service(Chrisliebot bot, String identifier) {
		
		// message content is still delivered for direct messages and messages that mention the bot
		Message.suppressContentIntentWarning();
		
		var jda = JDABuilder.create(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EXPRESSIONS)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
				.setEventManager(new AnnotatedEventManager())
				.setActivity(Activity.playing("mit dir"))
				.build();
		return new DiscordService(bot, jda, identifier, updateSlashCommands);
	}
}
