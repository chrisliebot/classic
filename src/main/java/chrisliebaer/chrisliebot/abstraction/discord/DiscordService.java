package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
public class DiscordService implements ChrislieService {
	
	public static final String PREFIX_GUILD_CHANNEL = "G:";
	public static final String PREFIX_PRIVATE_CHANNEL = "P:";
	
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
	public Optional<ChrislieChannel> channel(String identifier) {
		if (identifier.startsWith(PREFIX_GUILD_CHANNEL)) {
			var channel = jda.getGuildChannelById(identifier.substring(PREFIX_GUILD_CHANNEL.length()));
			return channel == null ? Optional.empty() : Optional.of(new DiscordGuildChannel(this, (TextChannel) channel));
		}
		if (identifier.startsWith(PREFIX_PRIVATE_CHANNEL)) {
			var user = jda.getUserById(identifier.substring(PREFIX_PRIVATE_CHANNEL.length()));
			if (user == null)
				return Optional.empty();
			var future = user.openPrivateChannel().submit();
			try {
				var channel = future.get();
				return Optional.of(new DiscordPrivateChannel(this, channel));
			} catch (InterruptedException ignore) {
				Thread.currentThread().interrupt();
				return Optional.empty();
			} catch (ExecutionException | CancellationException ignore) {
				return Optional.empty();
			}
		}
		
		return Optional.empty();
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
		return service.service() instanceof DiscordService;
	}
}
