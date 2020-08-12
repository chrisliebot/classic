package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Optional;

public interface DiscordChannel extends ChrislieChannel {
	
	public MessageChannel messageChannel();
	
	@Override
	public Optional<DiscordGuild> guild();
}
