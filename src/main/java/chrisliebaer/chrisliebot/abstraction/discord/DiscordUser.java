package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.User;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class DiscordUser implements ChrislieUser {
	
	@Getter private DiscordService service;
	@Getter private User user;
	
	public DiscordUser(@NonNull DiscordService service, @NonNull User user) {
		this.service = service;
		this.user = user;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DiscordUser that = (DiscordUser) o;
		return user.equals(that.user);
	}
	
	@Override
	public int hashCode() {
		return user.hashCode();
	}
	
	@Override
	public String displayName() {
		return user.getName();
	}
	
	@Override
	public String identifier() {
		return user.getId();
	}
	
	@Override
	public String mention() {
		return user.getAsMention();
	}
	
	@Override
	public Optional<DiscordChannel> directMessage() {
		var future = user.openPrivateChannel().submit();
		try {
			var channel = future.get();
			return Optional.of(new DiscordChannel(service, channel));
		} catch (InterruptedException ignore) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException | CancellationException ignore) {
			return Optional.empty();
		}
	}
}
