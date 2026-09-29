package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@ToString
public class DiscordGuild implements ChrislieGuild {
	
	@Getter private DiscordService service;
	@Getter private Guild guild;
	
	@Override
	public String displayName() {
		return guild.getName();
	}
	
	@Override
	public String identifier() {
		return guild.getId();
	}
	
	@Override
	public Collection<DiscordGuildChannel> channels() {
		return Stream.concat(guild.getChannels().stream(), guild.getThreadChannels().stream())
				.filter(GuildMessageChannel.class::isInstance)
				.map(channel -> new DiscordGuildChannel(service, (GuildMessageChannel) channel))
				.collect(Collectors.toList());
	}
	
	@Override
	public boolean isMember(ChrislieUser user) {
		return member(user.identifier()).isPresent();
	}
	
	/**
	 * @param userId The id of the user.
	 * @return The member or an empty optional if the user is not part of this guild.
	 * @see DiscordService#member(Guild, String)
	 */
	public Optional<Member> member(String userId) {
		return service.member(guild, userId);
	}
}
