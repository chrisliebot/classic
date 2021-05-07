package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Optional;

public interface DiscordChannel extends ChrislieChannel {
	
	public MessageChannel messageChannel();
	
	@Override
	public Optional<DiscordGuild> guild();
	
	@Override
	public default ChrislieOutput output(LimiterConfig limiterConfig) {
		return new DiscordOutput(messageChannel());
	}
	
	public default ChrislieOutput output(LimiterConfig limiterConfig, DiscordMessage source) {
		return new DiscordOutput(messageChannel(), source);
	}
	
}
