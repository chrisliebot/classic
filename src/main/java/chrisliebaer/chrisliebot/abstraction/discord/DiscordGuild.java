package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Collection;
import java.util.stream.Collectors;

@AllArgsConstructor
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
		return guild.getTextChannels().stream()
				.map(channel -> new DiscordGuildChannel(service, channel))
				.collect(Collectors.toList());
	}
}
