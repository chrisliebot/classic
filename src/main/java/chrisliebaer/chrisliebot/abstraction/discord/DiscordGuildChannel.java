package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DiscordGuildChannel implements DiscordChannel {
	
	@Getter private DiscordService service;
	@Getter private TextChannel channel;
	
	public DiscordGuildChannel(@NonNull DiscordService service, @NonNull TextChannel channel) {
		this.service = service;
		this.channel = channel;
	}
	
	@Override
	public String identifier() {
		return DiscordService.PREFIX_GUILD_CHANNEL + channel.getId();
	}
	
	@Override
	public Optional<DiscordGuild> guild() {
		return Optional.of(new DiscordGuild(service, channel.getGuild()));
	}
	
	@Override
	public List<DiscordUser> users() {
		return channel.getMembers().stream()
				.map(member -> new DiscordUser(service, member.getUser()))
				.collect(Collectors.toList());
	}
	
	@Override
	public Optional<DiscordUser> resolve(String callName) {
		throw new RuntimeException("not yet implemented"); // TODO
	}
	
	@Override
	public String displayName() {return channel.getName();}
	
	@Override
	public boolean isDirectMessage() {return false;}
	
	@Override
	public boolean isNSFW() {return channel.isNSFW();}
	
	@Override
	public MessageChannel messageChannel() {
		return channel;
	}
}
