package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class DiscordService implements ChrislieService {
	
	@Getter private JDA jda;
	@Getter private String identifier;
	
	@Setter private Consumer<ChrislieMessage> sink;
	
	@SuppressWarnings("ThisEscapedInObjectConstruction")
	public DiscordService(JDA jda, String identifier) {
		this.jda = jda;
		this.identifier = identifier;
		
		jda.addEventListener(this);
	}
	
	@Override
	public void awaitReady() throws InterruptedException {
		jda.awaitReady();
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
	
	@Override
	public Optional<DiscordGuild> guild(String identifier) {
		return Optional.ofNullable(jda.getGuildById(identifier))
				.map(guild -> new DiscordGuild(this, guild));
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
	public void exit() throws ServiceException {
		final var helper = new Object() {
			private final CountDownLatch latch = new CountDownLatch(1);
			
			@SubscribeEvent
			public void onShutdown(ShutdownEvent ev) {
				latch.countDown();
			}
		};
		
		// register helper to keep track shutdown event
		jda.addEventListener(helper);
		jda.removeEventListener(this);
		jda.shutdown();
		
		// wait for shutdown event to occur
		try {
			helper.latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ServiceException("got interrupted while waiting for jda shutdown", e);
		}
	}
	
	public static boolean isDiscord(ServiceAttached service) {
		return service instanceof DiscordService;
	}
}
