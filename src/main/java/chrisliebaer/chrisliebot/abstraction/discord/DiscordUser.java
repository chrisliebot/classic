package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.User;

import java.util.Optional;

public class DiscordUser implements ChrislieUser {
	
	@Getter private DiscordService service;
	@Getter private User user;
	
	public DiscordUser(@NonNull DiscordService service, @NonNull User user) {
		this.service = service;
		this.user = user;
	}
	
	@Override
	public String displayName() {
		return user.getName();
	}
	
	@Override
	public Optional<String> identifier() {
		return Optional.of(user.getId());
	}
	
	@Override
	public String mention() {
		return user.getAsMention();
	}
	
	@Override
	public boolean isAdmin() {
		return service.isAdmin(user);
	}
	
	@Override
	public ChrislieChannel directMessage() {
		// TODO: make this shit an optional since this might not always work (blocked for instance)
		return new DiscordChannel(service, user.openPrivateChannel().complete());
	}
}
