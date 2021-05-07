package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
public class DiscordService implements ChrislieService {
	
	public static final String PREFIX_GUILD_CHANNEL = "G:";
	public static final String PREFIX_PRIVATE_CHANNEL = "P:";
	
	@Getter private Chrisliebot bot;
	@Getter private JDA jda;
	@Getter private String identifier;
	
	@Setter private Consumer<ChrislieMessage> sink;
	
	@SuppressWarnings("ThisEscapedInObjectConstruction")
	public DiscordService(Chrisliebot bot, JDA jda, String identifier) {
		this.bot = bot;
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
		@SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter") final var helper = new Object() {
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
	
	public void traceMessage(@NonNull Message source, @NonNull Message result) {
		var sql = """
					INSERT INTO `discord_message_trace`
					(
						`channelId`, `messageId`,
						`sourceGuildId`, `sourceChannelId`, `sourceMessageId`, `sourceUserNickname`, `sourceUserDiscriminator`, `sourceUserId`,
						`sourceContent`
					) VALUES (
						?, ?, ?, ?, ?, ?, ?, ?, ?
					)
				""";
		
		try (var conn = bot.sharedResources().dataSource().getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, result.getChannel().getIdLong());
			stmt.setLong(2, result.getIdLong());
			
			if (source.isFromGuild())
				stmt.setLong(3, source.getGuild().getIdLong());
			else
				stmt.setNull(3, Types.BIGINT);
			
			stmt.setLong(4, source.getChannel().getIdLong());
			stmt.setLong(5, source.getIdLong());
			
			var user = source.getAuthor();
			stmt.setString(6, user.getName());
			stmt.setInt(7, Integer.parseInt(user.getDiscriminator().substring(1)));
			stmt.setLong(8, user.getIdLong());
			stmt.setString(9, source.getContentRaw());
			
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("error inserting message trace to database", e);
		}
	}
	
	public Optional<TraceMessageSource> fetchMessageTrace(@NonNull Message msg) {
		var sql = """
					SELECT `sourceGuildId`, `sourceChannelId`, `sourceMessageId`, `sourceUserNickname`, `sourceUserDiscriminator`, `sourceUserId`, `sourceContent`
					FROM discord_message_trace
					WHERE channelId = ? AND messageId = ?
					LIMIT 1
				""";
		
		try (var conn = bot.sharedResources().dataSource().getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, msg.getChannel().getIdLong());
			stmt.setLong(2, msg.getIdLong());
			
			try (var rs = stmt.executeQuery()) {
				if (rs.next())
					return Optional.of(TraceMessageSource.fromResultSet(rs));
			}
		} catch (SQLException e) {
			log.error("error fetching trace from database", e);
		}
		return Optional.empty();
	}
	
	public static boolean isDiscord(ServiceAttached service) {
		return service.service() instanceof DiscordService;
	}
}
