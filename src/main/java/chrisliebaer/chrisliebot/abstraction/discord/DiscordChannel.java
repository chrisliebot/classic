package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.Optional;

public interface DiscordChannel extends ChrislieChannel {
	
	public MessageChannel messageChannel();
	
	@Override
	public Optional<DiscordGuild> guild();
	
	@Override
	public DiscordService service();
	
	@Override
	public default ChrislieOutput output(LimiterConfig limiterConfig) {
		return new DiscordChannelOutput(service(), messageChannel());
	}
	
	public default ChrislieOutput output(LimiterConfig limiterConfig, DiscordMessage source) {
		return new DiscordChannelOutput(service(), messageChannel(), source);
	}
	
	/**
	 * @param service The service the channel belongs to.
	 * @param channel A Discord channel.
	 * @return The guild or private channel representing the given channel.
	 * @throws IllegalArgumentException If the channel is neither a guild message channel nor a private channel.
	 */
	public static DiscordChannel of(DiscordService service, MessageChannel channel) {
		return switch (channel) {
			case GuildMessageChannel guildChannel -> new DiscordGuildChannel(service, guildChannel);
			case PrivateChannel privateChannel -> new DiscordPrivateChannel(service, privateChannel);
			default -> throw new IllegalArgumentException("message was sent in unsupported channel type: " + channel.getType());
		};
	}
}
