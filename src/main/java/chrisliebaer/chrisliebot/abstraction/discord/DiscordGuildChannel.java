package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.List;
import java.util.Optional;

public class DiscordGuildChannel implements DiscordChannel {
	
	@Getter private DiscordService service;
	@Getter private GuildMessageChannel channel;
	
	public DiscordGuildChannel(@NonNull DiscordService service, @NonNull GuildMessageChannel channel) {
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
	public String mention() {
		return channel.getAsMention();
	}
	
	/**
	 * Listing the members of a guild channel requires the privileged members intent.
	 *
	 * @throws UnsupportedOperationException Always.
	 */
	@Override
	public List<DiscordUser> users() {
		throw new UnsupportedOperationException("listing members of a guild channel requires the privileged members intent");
	}
	
	@Override
	public Optional<DiscordUser> user(String identifier) {
		return new DiscordGuild(service, channel.getGuild()).member(identifier)
				.filter(member -> member.hasPermission(channel, Permission.VIEW_CHANNEL))
				.map(member -> new DiscordUser(service, member.getUser()));
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
	public boolean isNSFW() {
		var ageRestricted = channel instanceof ThreadChannel thread ? thread.getParentChannel() : channel;
		return ageRestricted instanceof IAgeRestrictedChannel restricted && restricted.isNSFW();
	}
	
	@Override
	public MessageChannel messageChannel() {
		return channel;
	}
	
	@Override
	public boolean canTalk() {
		return channel.canTalk();
	}
}
