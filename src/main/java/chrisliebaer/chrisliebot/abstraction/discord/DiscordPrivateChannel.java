package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Optional;

public class DiscordPrivateChannel implements DiscordChannel {
	
	@Getter private DiscordService service;
	@Getter private PrivateChannel channel;
	
	private User user;
	
	public DiscordPrivateChannel(@NonNull DiscordService service, @NonNull PrivateChannel channel) {
		this.service = service;
		this.channel = channel;
		
		user = channel.getUser();
	}
	
	@Override
	public String identifier() {
		return DiscordService.PREFIX_PRIVATE_CHANNEL + user.getId(); // use user id as identifier since we need to reopen this channel
	}
	
	@Override
	public List<DiscordUser> users() {
		return List.of(new DiscordUser(service, user));
	}
	
	@Override
	public Optional<DiscordGuild> guild() {return Optional.empty();}
	
	@Override
	public Optional<DiscordUser> resolve(String callName) {
		throw new RuntimeException("not yet implemented"); // TODO
	}
	
	@Override
	public String displayName() {return channel.getName();}
	
	@Override
	public boolean isDirectMessage() {return true;}
	
	@Override
	public MessageChannel messageChannel() {
		return channel;
	}
}
