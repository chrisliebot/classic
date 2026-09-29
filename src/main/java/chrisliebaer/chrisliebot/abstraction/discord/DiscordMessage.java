package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DiscordMessage implements ChrislieMessage {
	
	@Getter private DiscordService service;
	@Getter private MessageReceivedEvent ev;
	
	@Getter private DiscordChannel channel;
	
	public DiscordMessage(@NonNull DiscordService service, @NonNull MessageReceivedEvent ev) {
		this.service = service;
		this.ev = ev;
		
		channel = DiscordChannel.of(service, ev.getChannel());
	}
	
	@Override
	public DiscordUser user() {
		return new DiscordUser(service, ev.getAuthor());
	}
	
	@Override
	public String message() {
		return ev.getMessage().getContentRaw();
	}
	
	@Override
	public ChrislieOutput reply(LimiterConfig limiter) {
		return channel.output(limiter, this);
	}
}
