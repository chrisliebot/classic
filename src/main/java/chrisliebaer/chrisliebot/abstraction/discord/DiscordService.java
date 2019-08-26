package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DiscordService implements ChrislieService {
	
	@Getter private JDA jda;
	@Getter private List<String> admins;
	
	@Setter private Consumer<ChrislieMessage> sink;
	
	public DiscordService(JDA jda, List<String> admins) {
		this.jda = jda;
		this.admins = admins;
		
		jda.addEventListener(this);
	}
	
	@Override
	public Optional<DiscordChannel> channel(String identifier) {
		TextChannel channel = jda.getTextChannelById(identifier);
		
		return channel == null ? Optional.empty() : Optional.of(new DiscordChannel(this, channel));
	}
	
	@Override
	public Optional<DiscordUser> user(String identifier) {
		User user = jda.getUserById(identifier);
		return user == null ? Optional.empty() : Optional.of(new DiscordUser(this, user));
	}
	
	@SubscribeEvent
	public void onMessage(MessageReceivedEvent ev) {
		if (ev.getAuthor().isBot())
			return;
		
		var sink = this.sink;
		if (sink != null)
			sink.accept(new DiscordMessage(this, ev));
	}
	
	@Override
	public void reconnect() {}
	
	@Override
	public void exit() throws Exception {
		jda.removeEventListener(this);
		jda.shutdown();
	}
	
	public boolean isAdmin(User user) {
		return admins.contains(user.getId());
	}
	
}
