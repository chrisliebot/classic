package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DiscordMessage implements ChrislieMessage {
	
	@Getter private DiscordService service;
	@Getter private MessageReceivedEvent ev;
	
	@Getter private ChrislieChannel channel;
	
	public DiscordMessage(@NonNull DiscordService service, @NonNull MessageReceivedEvent ev) {
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
		return new DiscordUser(service, ev.getAuthor());
	}
	
	@Override
	public String message() {
		return ev.getMessage().getContentRaw();
	}
}
