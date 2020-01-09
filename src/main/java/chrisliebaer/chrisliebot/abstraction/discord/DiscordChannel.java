package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscordChannel implements ChrislieChannel { // TODO: we should probably split this into two implementations for guild and no guild channels
	
	@Getter private DiscordService service;
	@Getter private MessageChannel channel;
	
	public DiscordChannel(@NonNull DiscordService service, @NonNull MessageChannel channel) {
		this.service = service;
		this.channel = channel;
	}
	
	@Override
	public String displayName() {
		return channel.getName();
	}
	
	@Override
	public String identifier() {
		return channel.getId();
	}
	
	@Override
	public boolean isDirectMessage() {
		return channel instanceof PrivateChannel;
	}
	
	@Override
	public boolean isNSFW() {
		if (channel instanceof TextChannel) {
			TextChannel c = (TextChannel) channel;
			return c.isNSFW();
		}
		return false;
	}
	
	@Override
	public List<DiscordUser> users() {
		if (channel instanceof PrivateChannel) {
			PrivateChannel privateChannel = (PrivateChannel) channel;
			return List.of(new DiscordUser(service, privateChannel.getUser()));
		} else if (channel instanceof GuildChannel) {
			GuildChannel guildChannel = (GuildChannel) channel;
			return guildChannel.getMembers().stream()
					.map(member -> new DiscordUser(service, member.getUser()))
					.collect(Collectors.toList());
		} else {
			throw new UnsupportedOperationException("this channel type does not support accessing it's users");
		}
	}
	
	@Override
	public Optional<DiscordUser> resolve(String callName) {
		throw new RuntimeException("not yet implemented");
	}
	
	@Override
	public Optional<DiscordGuild> guild() {
		if (channel instanceof GuildChannel) {
			GuildChannel guildChannel = (GuildChannel) channel;
			return Optional.of(new DiscordGuild(service, guildChannel.getGuild()));
		}
		return Optional.empty();
	}
	
	@Override
	public ChrislieOutput output(LimiterConfig limiterConfig) {
		return new DiscordOutput(channel);
	}
}
