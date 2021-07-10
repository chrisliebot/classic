package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class DiscordSlashCommandMessage implements ChrislieMessage {
	
	@Getter private final DiscordService service;
	@Getter private final SlashCommandEvent ev;
	
	@Getter private final DiscordChannel channel;
	
	public DiscordSlashCommandMessage(@NonNull DiscordService service, @NonNull SlashCommandEvent ev) {
		this.service = service;
		this.ev = ev;
		
		switch (ev.getChannelType()) {
			case TEXT -> channel = new DiscordGuildChannel(service, ev.getTextChannel());
			case PRIVATE -> channel = new DiscordPrivateChannel(service, ev.getPrivateChannel());
			default -> throw new RuntimeException("message was sent in unkown channel type");
		}
	}
	
	@Override
	public DiscordUser user() {
		return new DiscordUser(service, ev.getUser());
	}
	
	@Override
	public String message() {
		return ev.toString();
	}
	
	@Override
	public ChrislieOutput reply(LimiterConfig limiter) {
		return channel.output(limiter);
	}
}
