package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DiscordMessage implements ChrislieMessage {
	
	@Getter private DiscordService service;
	@Getter private MessageReceivedEvent ev;
	
	public DiscordMessage(@NonNull DiscordService service, @NonNull MessageReceivedEvent ev) {
		this.service = service;
		this.ev = ev;
	}
	
	@Override
	public DiscordChannel channel() {
		return new DiscordChannel(service, ev.getChannel());
	}
	
	@Override
	public DiscordUser user() {
		return new DiscordUser(service, ev.getAuthor());
	}
	
	@Override
	public String message() {
		return ev.getMessage().getContentRaw();
	}
	
}
