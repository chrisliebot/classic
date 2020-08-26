package chrisliebaer.chrisliebot.command.timer;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.flex.CommonFlex;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.sql.DataSource;
import javax.validation.constraints.Positive;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* v3 features TODO
 * upper limit for duration, number of timers (can be combined with scope based map used for other limits I can't remember)
 * permission for accessing other tasks / and deleting them
 * order timer output by expiration time
 * implement ^ selector for last expired timer
 */

/**
 * In order to simplify documentation of this class we introduce a few concepts first.
 * <ul>
 *     <li>Runtime Timer: A timer that has been converted into a TimerTask and is currently active in main memory.</li>
 *     <li>Prefetch Duration: The amount of duration a timer must have left before it is converted into a Runtime Timer on the next database cycle.</li>
 *     <li>Hot Timer: All pending timers that will be due during the current Prefetch Duration.</li>
 * </ul>
 * <p>
 * In order to keep the memory footprint low and the logic in this class simple, most operations are perfomed on the database only. A peridic task is polling a list of Hot Timers from the database, converting them into Runtime Timer. All operations are still performed on the database, while any potential Runtime Timer is removed from main memory and refetched to stay synchronized with the database representation of it's timer.
 */
@Slf4j
public class TimerCommand implements ChrislieListener.Command {
	
	private static final Timestamp UNIX_EPOCH_SECOND_1 = Timestamp.from(Instant.ofEpochSecond(1));
	
	private static final String ERROR_TIMER_UNKOWN_OR_RESTRICTED = "Diesen Timer kenne ich nicht oder du darfst ihn nicht bearbeiten.";
	
	private static final String ENCODER_ALPHABET = "abcdefghkmnopqrstuvwxyz123456789";
	private static final int ENCODER_ALPHABET_LOG = 5; // log_2(alphabet.length)
	private static final long ENCODER_BITMASK = 0b00011111;
	
	private static final long PURGE_INTERVAL = 60 * 60 * 1000;
	
	private static final String SHORTHAND_LAST_EXPIRED = "^";
	private static final String SHORTHAND_LAST_CREATED = ".";
	private static final String SHORTHAND_UPCOMING = "-";
	
	// TODO: put these in code and replace with exception based error messages later
	private static final ErrorOutputBuilder ERROR_DATE_IN_PAST = ErrorOutputBuilder.generic("Dieser Zeitpunkt liegt in der Vergangenheit.");
	private static final ErrorOutputBuilder ERROR_INVALID_DATE = ErrorOutputBuilder.generic("Dieses Datum habe ich leider nicht verstanden.");
	
	private boolean shutdown;
	
	private Config cfg;
	
	private Chrisliebot bot;
	private ContextResolver resolver;
	
	private ScheduledExecutorService timer;
	private DataSource dataSource;
	
	private final Map<Long, ScheduledTimer> runtimeTimer = new HashMap<>();
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("10 min Pizza|list, info|delete|restore <id>, snooze <id> 2 days. Zusätzlich gibt es folgende Kürzel für <id>: ^ letzter abgelaufener Timer, . letzter angelegter Timer, - nächster fälliger Timer.");
	}
	
	@Override
	public synchronized void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public synchronized void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		this.dataSource = bot.sharedResources().dataSource();
		this.resolver = resolver;
		this.timer = bot.sharedResources().timer();
	}
	
	@Override
	public synchronized void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		
		// while refresh timer could take care of that, we do it here to provide exception handling to chrisliebot framework
		try {
			refreshRuntimeTimer();
		} catch (SQLException e) {
			throw new ListenerException("failed to load timers from database", e);
		}
		
		timer.scheduleWithFixedDelay(() -> {
			try {
				refreshRuntimeTimer();
			} catch (SQLException e) {
				log.warn("failed to refresh timers from database", e);
			}
		}, 0, cfg.prefetchInterval, TimeUnit.MILLISECONDS);
		timer.scheduleWithFixedDelay(this::purgeExpired, 0, PURGE_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public synchronized void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		shutdown = true;
		
		runtimeTimer.values().forEach(t -> t.future.cancel(false));
		runtimeTimer.clear(); // clear map so we don't get confused in case we need to debug anything
	}
	
	@Override
	public synchronized void execute(Invocation invc) throws ListenerException {
		var arg = invc.arg();
		
		try {
			if (arg.startsWith("list")) {
				listCommand(invc);
				return;
			} else if (arg.startsWith("delete") || arg.startsWith("del")) {
				deleteCommand(invc);
				return;
			} else if (arg.startsWith("info") || arg.startsWith("i")) {
				infoCommand(invc);
				return;
			} else if (arg.startsWith("snooze")) {
				snoozeCommand(invc);
				return;
			} else if (arg.startsWith("restore")) {
				restoreCommand(invc);
				return;
			}
		} catch (IdParseException e) {
			ErrorOutputBuilder.generic("Diese Timer Id konnte ich nicht verarbeiten.").write(invc).send();
			return;
		}
		
		var pair = shrinkingParse(arg, CommonFlex.ZONE_ID().getOrFail(invc));
		if (pair.isEmpty()) {
			ERROR_INVALID_DATE.write(invc).send();
			return;
		}
		
		var due = pair.get().getLeft();
		var text = pair.get().getRight();
		
		if (due.isBefore(Instant.now())) {
			ERROR_DATE_IN_PAST.write(invc).send();
			return;
		}
		
		var message = invc.msg();
		var channel = message.channel();
		
		// create timer
		TimerInfo timerInfo = new TimerInfo();
		timerInfo.service = message.service().identifier();
		timerInfo.user = message.user().identifier();
		timerInfo.channel = channel.identifier();
		timerInfo.text = text;
		timerInfo.creation = Instant.now();
		timerInfo.due = due;
		
		try {
			createTimer(timerInfo);
			
			// pulling in case timer is already hot
			refreshRuntimeTimer();
		} catch (SQLException e) {
			throw new ListenerException("failed to store new timer in database", e);
		}
		
		var reply = invc.reply();
		reply.title("Neuer Timer angelegt");
		formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
		reply.send();
	}
	
	private synchronized void listCommand(Invocation invc) throws ListenerException {
		String sql = "SELECT * FROM timer WHERE service = ? AND user = ? AND deleted = FALSE " +
				"ORDER BY COALESCE(snooze, due)";
		
		// while we are only requesting a users tasks, we also need to make sure they are safe to be displayed in the current context
		Predicate<TimerInfo> pred = accessPredicate(invc.msg());
		
		List<TimerInfo> timerList = new ArrayList<>();
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, invc.service().identifier());
			stmt.setString(2, invc.msg().user().identifier());
			
			try (var rs = stmt.executeQuery()) {
				while (rs.next()) {
					var timerInfo = createTimerInfo(rs);
					if (pred.test(timerInfo)) // reject if private in current context
						timerList.add(timerInfo);
				}
			}
		} catch (SQLException e) {
			throw new ListenerException("failed to access users timer", e);
		}
		
		if (timerList.isEmpty()) {
			ErrorOutputBuilder.generic("Du hast keine Timer, die ich dir hier anzeigen darf.").write(invc).send();
			return;
		}
		
		// TODO: sort list
		var reply = invc.reply();
		reply.title("Du hast hier zur Zeit " + timerList.size() + " Timer");
		var joiner = reply.description().joiner(", ");
		
		for (var timer : timerList) {
			var id = encodeTimer(timer.id);
			var abbrev = StringUtils.abbreviate(timer.text, cfg.abbrevLength);
			
			joiner.seperator().appendEscape(id, ChrislieFormat.BOLD).appendEscape(": ").appendEscape(abbrev);
		}
		reply.send();
	}
	
	private synchronized void deleteCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg()); // TODO: rework all methods to throw exception if argument is empty, part of error handling rework
		long id = decodeTimer(arg);
		
		try (var conn = dataSource.getConnection()) {
			// we just fetch the timer and check if the user is allowed to change it, easier then checking inside the query
			var maybeTimerInfo = getTimerFromDb(conn, id).filter(accessPredicate(invc.msg()));
			if (maybeTimerInfo.isEmpty() || maybeTimerInfo.get().deleted) {
				ErrorOutputBuilder.generic(ERROR_TIMER_UNKOWN_OR_RESTRICTED).write(invc).send();
				return;
			}
			var timerInfo = maybeTimerInfo.get();
			
			// we know the user is allowed to modify the timer and we know it exists, so we simply update it without further checks
			String sql = "UPDATE timer SET deleted = TRUE WHERE id = ?";
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				stmt.execute();
			}
			
			// just assume it was a runtime timer, does nothing if assumption is wrong
			removeRuntime(id);
			
			var reply = invc.reply();
			reply.title("Timer gelöscht");
			formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
			reply.send();
		} catch (SQLException e) {
			throw new ListenerException("failed to delete timer", e);
		}
	}
	
	/**
	 * @param conn The connection to use. Since this method is often part of more complex interactions.
	 * @param id   The timer id to fetch from the database.
	 * @return An optional TimerInfo if the id resolved to a valid timer.
	 * @throws SQLException If a database error occurs.
	 */
	private synchronized Optional<TimerInfo> getTimerFromDb(Connection conn, long id) throws SQLException {
		String sql = "SELECT * FROM timer WHERE id = ?";
		try (var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, id);
			
			try (var rs = stmt.executeQuery()) {
				if (!rs.next())
					return Optional.empty();
				
				var timerInfo = createTimerInfo(rs);
				return Optional.of(timerInfo);
			}
		}
	}
	
	private synchronized void infoCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		long id = decodeTimer(arg);
		
		try (var conn = dataSource.getConnection()) {
			var maybeTimerInfo = getTimerFromDb(conn, id).filter(accessPredicate(invc.msg()));
			if (maybeTimerInfo.isEmpty() || maybeTimerInfo.get().deleted) {
				ErrorOutputBuilder.generic(ERROR_TIMER_UNKOWN_OR_RESTRICTED).write(invc).send();
				return;
			}
			
			var reply = invc.reply();
			reply.title("Timerinformationen");
			formatTimerOutput(reply, maybeTimerInfo.get(), invc.ref().flexConf(), false);
			reply.send();
		} catch (SQLException e) {
			throw new ListenerException("failed to fetch timer from database", e);
		}
	}
	
	private synchronized void restoreCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		var id = decodeTimer(arg);
		
		try (var conn = dataSource.getConnection()) {
			var maybeTimerInfo = getTimerFromDb(conn, id).filter(accessPredicate(invc.msg()));
			if (maybeTimerInfo.isEmpty()) {
				ErrorOutputBuilder.generic(ERROR_TIMER_UNKOWN_OR_RESTRICTED).write(invc).send();
				return;
			}
			var timerInfo = maybeTimerInfo.get();
			
			if (!timerInfo.deleted) {
				ErrorOutputBuilder.generic("Dieser Timer wurde gar nicht gelöscht.").write(invc).send();
				return;
			}
			
			if (timerInfo.nextDue().isBefore(Instant.now())) {
				ErrorOutputBuilder.generic("Dieser Timer ist abgelaufen und muss daher genoozed werden.").write(invc).send();
				return;
			}
			
			String sql = "UPDATE timer SET deleted = FALSE WHERE id = ?";
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				stmt.execute();
			}
			
			// the restored timer could be hot, so we pull from database
			refreshRuntimeTimer();
			
			var reply = invc.reply();
			reply.title("Timer wiederhergestellt");
			formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
			reply.send();
			
		} catch (SQLException e) {
			throw new ListenerException("failed to restore timer", e);
		}
	}
	
	private synchronized void snoozeCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		var args = arg.split(" ", 2);
		if (args.length != 2) {
			// TODO: merge with other parsing errors in info, delete, restore
			ErrorOutputBuilder.generic("Du hast nicht angegeben, auf welchen Zeitpunkt ich den Timer verschieben soll.").write(invc).send();
			return;
		}
		var id = decodeTimer(args[0]);
		
		// parse and validate new instant
		var maybeWhen = parse(args[1], CommonFlex.ZONE_ID().getOrFail(invc));
		if (maybeWhen.isEmpty()) {
			ERROR_INVALID_DATE.write(invc).send();
			return;
		}
		var when = maybeWhen.get();
		
		if (when.isBefore(Instant.now())) {
			ERROR_DATE_IN_PAST.write(invc).send();
			return;
		}
		
		try (var conn = dataSource.getConnection()) {
			var maybeTimerInfo = getTimerFromDb(conn, id).filter(accessPredicate(invc.msg()));
			if (maybeTimerInfo.isEmpty()) {
				ErrorOutputBuilder.generic(ERROR_TIMER_UNKOWN_OR_RESTRICTED).write(invc).send();
				return;
			}
			var timerInfo = maybeTimerInfo.get();
			
			String sql = "UPDATE timer SET snooze = ?, deleted = FALSE, snoozecount = snoozecount + 1 WHERE id = ?";
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.setTimestamp(1, Timestamp.from(when));
				stmt.setLong(2, id);
				
				stmt.execute();
			}
			
			// snooze may move timer from hot to cold, so we potentially remove it
			removeRuntime(id);
			
			// snooze may also move timer into hot state, so we need to pull after that, it's important to remove the timer first
			refreshRuntimeTimer();
			
			// timer got updated, so we need to fetch new data from database
			timerInfo = getTimerFromDb(conn, id).orElseThrow(); // we just updated it, we know it exists
			
			var reply = invc.reply();
			reply.title("Der Timer wurde erfolgreich verschoben");
			formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
			reply.send();
		} catch (SQLException e) {
			throw new ListenerException("failed to snooze timer", e);
		}
	}
	
	private void formatTimerOutput(ChrislieOutput out, TimerInfo timerInfo, FlexConf flex, boolean due) throws ListenerException {
		var zoneId = CommonFlex.ZONE_ID().getOrFail(flex);
		var formater = CommonFlex.DATE_TIME_FORMAT().getOrFail(flex);
		
		var when = timerInfo.nextDue().atZone(zoneId).format(formater);
		var creation = timerInfo.creation.atZone(zoneId).format(formater);
		var id = encodeTimer(timerInfo.id);
		
		var wtf = Duration.between(Instant.now(), timerInfo.nextDue());
		
		var duration = C.format(wtf);
		
		Optional<ChrislieService> service = bot.service(timerInfo.service);
		Optional<ChrislieUser> user = service.flatMap(s -> s.user(timerInfo.user));
		Optional<ChrislieChannel> channel = service.flatMap(s -> s.channel(timerInfo.channel));
		Optional<ChrislieGuild> guild = channel.flatMap(ChrislieChannel::guild);
		
		out.description(timerInfo.text);
		out.field("Id", id);
		user.ifPresent(u -> out.field("Besitzer", u.mention()));
		channel.ifPresent(c -> out.field("Channel", c.displayName()));
		guild.ifPresent(g -> out.field("Gilde", g.displayName()));
		out.field("Fällig", when);
		
		if (!due)
			out.field("Dauer", duration);
		
		var convert = out.convert();
		if (due && user.isPresent()) {
			convert.append(user.get().mention()).appendEscape(": ");
		}
		
		convert.appendEscapeSub("${title} - ")
				.appendEscape("Text: ")
				.appendEscape(timerInfo.text)
				.appendEscape(", Id: ").appendEscape(id)
				.appendEscape(", Besitzer: ")
				.appendEscape(user.map(ChrislieUser::mention).orElse("Unbekannt"), ChrislieFormat.HIGHLIGHT)
				.appendEscape(", Fällig: ").appendEscape(when, ChrislieFormat.HIGHLIGHT);
		
		if (!due)
			convert.appendEscape(", Dauer: ").appendEscape(duration, ChrislieFormat.HIGHLIGHT);
		
		if (timerInfo.snoozeCount > 0) {
			out.field("Snoozezähler", String.valueOf(timerInfo.snoozeCount));
			convert.appendEscape(", Snoozezähler: ").appendEscape(String.valueOf(timerInfo.snoozeCount));
		}
	}
	
	/**
	 * Removes the first word of the given string including any following space and returns whats left (if anything).
	 *
	 * @param arg The string to truncate.
	 * @return The remaining string. Never null.
	 */
	private static String getSubCommandArg(String arg) { // TODO move this into it's own command parser
		var args = arg.split(" ", 2);
		if (args.length > 1)
			return args[1];
		return "";
	}
	
	/**
	 * Creates a Predicate from a ChrislieMessage. The rules of this predicate are hardcoded and intented to prevent
	 * users from accesing not only other users timers but also leaking their own timers on guilds or channels by
	 * accident.
	 *
	 * @param msg The message that should be used for the accessibility check.
	 * @return A predicate that matches on all timers that are allowed to be displayed in the context of the given
	 * message.
	 */
	private static Predicate<TimerInfo> accessPredicate(ChrislieMessage msg) {
		var user = msg.user();
		var channel = msg.channel();
		var guild = channel.guild();
		
		Predicate<TimerInfo> pred = timer -> timer.service.equals(user.service().identifier()) && timer.user.equals(user.identifier());
		
		if (guild.isPresent()) { // if guild is present, we filter by channel guilds
			
			var guildChannels = guild.get().channels().stream()
					.map(ChrislieChannel::identifier)
					.collect(Collectors.toSet());
			
			pred = pred.and(timer -> guildChannels.contains(timer.channel));
		} else if (!channel.isDirectMessage()) { // if not dm channel, we filter by channel
			pred = pred.and(timer -> channel.identifier().equals(timer.channel));
		}  // otherwise we show user all their timers
		
		return pred;
	}
	
	
	/**
	 * Called from timer thread to trigger timer output when timer is due.
	 *
	 * @param timerInfo The stored timer information for information retrieval.
	 * @throws ListenerException If the bot framework is unable to initialize the output.
	 */
	private synchronized void timerDue(TimerInfo timerInfo) throws ListenerException {
		// remove timer from runtime timers, if it fails, the timer was canceled while aquiring synchronisation lock
		if (!removeRuntime(timerInfo.id))
			return;
		
		// if shutdown, timer will survive until reboot
		if (shutdown)
			return;
		
		var maybeService = bot.service(timerInfo.service);
		if (maybeService.isEmpty()) {
			log.debug("service unknown, failed to deliver timer {}", timerInfo);
			return;
		}
		var service = maybeService.get();
		var maybeUser = service.user(timerInfo.user);
		
		if (maybeUser.isEmpty()) {
			log.debug("user unknown, failed to deliver timer {}", timerInfo);
			return;
		}
		var user = maybeUser.get();
		
		// if the original channel is not available, the message will be delivered via dm
		boolean dmRedirected = false;
		var maybeChannel = service.channel(timerInfo.channel);
		if (maybeChannel.isEmpty() || maybeChannel.get().user(user.identifier()).isEmpty()) { // channel doesn't exist or user is not in channel
			dmRedirected = true;
			maybeChannel = user.directMessage();
			if (maybeChannel.isEmpty()) {
				log.debug("failed to open dm channel");
				return;
			}
		}
		var channel = maybeChannel.get();
		var maybeRef = resolver.resolve(Selector::check, channel).listener(this);
		
		if (maybeRef.isEmpty()) {
			log.warn("missing ref in channel {} for delivery of timer {}", channel.displayName(), timerInfo);
			return;
		}
		var ref = maybeRef.get();
		var out = channel.output(LimiterConfig.of(ref.flexConf()));
		
		out.title("Es ist soweit");
		var plain = out.plain().append(maybeUser.get().mention());
		
		// let user know that this timer might be out of context
		if (dmRedirected)
			plain.appendEscape(" (Ich konnte leider den Originalchannel nicht mehr finden und hab dir deinen Timer daher privat geschickt.)");
		
		formatTimerOutput(out, timerInfo, ref.flexConf(), true);
		out.send();
		
		String sql = "UPDATE timer SET deleted = TRUE WHERE id = ?";
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, timerInfo.id);
			stmt.execute();
		} catch (SQLException e) {
			log.error("failed to mark deletion for due timer: {}", timerInfo, e);
		}
	}
	
	/**
	 * Calling this method will purge all timers that fullfil this instances purge requirement permanently from the
	 * database, making it impossible to restore them.
	 */
	private void purgeExpired() {
		
		// will also delete non expired timers if user was unreachable
		String sql = "DELETE FROM timer WHERE TIMESTAMPDIFF(SECOND, COALESCE(snooze, due), NOW()) > ?";
		
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, cfg.expire / 1000);
			
			var deleted = stmt.executeLargeUpdate();
			
			if (deleted > 0)
				log.debug("purged {} expired timers from database", deleted);
			
		} catch (SQLException e) {
			log.error("failed to delete expired timers", e);
		}
	}
	
	/**
	 * Stores the given timer in the database and associates it with an id.
	 *
	 * @param timerInfo The timer to store.
	 * @throws SQLException If an database error occurs.
	 */
	private synchronized void createTimer(TimerInfo timerInfo) throws SQLException {
		String sql = "INSERT INTO timer (service, user, channel, text, creation, due, snooze, snoozeCount, deleted)" +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			
			int i = 1;
			stmt.setString(i++, timerInfo.service);
			stmt.setString(i++, timerInfo.user);
			stmt.setString(i++, timerInfo.channel);
			stmt.setString(i++, timerInfo.text);
			
			stmt.setTimestamp(i++, Timestamp.from(timerInfo.creation));
			stmt.setTimestamp(i++, Timestamp.from(timerInfo.due));
			
			if (timerInfo.snooze == null)
				stmt.setNull(i++, Types.TIMESTAMP);
			else
				stmt.setTimestamp(i++, Timestamp.from(timerInfo.snooze));
			
			stmt.setInt(i++, timerInfo.snoozeCount);
			stmt.setBoolean(i, timerInfo.deleted);
			
			stmt.executeUpdate();
			
			try (var gen = stmt.getGeneratedKeys()) {
				if (gen.next()) {
					timerInfo.id = gen.getLong(1);
				} else {
					throw new SQLException("failed to retrieve timer insert id");
				}
			}
		}
	}
	
	/**
	 * Potentially invalidates the timer with the given id, purging it from main memory and ending it's Runtime Timer.
	 * If the timer is not a Runtime Timer, this method call will do nothing.
	 *
	 * @param id The id of the timer to invalidate.
	 * @return {@code true} if the timer was a Runtime Timer in which case most callers should call {@link
	 * #refreshRuntimeTimer()}.
	 */
	private synchronized boolean removeRuntime(long id) {
		var rt = runtimeTimer.remove(id);
		if (rt != null) {
			rt.future.cancel(false);
			return true;
		}
		return false;
	}
	
	/**
	 * Fetches all Hot Timers from the database and adds them to the current list of Runtime Timers if they are absent.
	 *
	 * @throws SQLException If a database operation fails.
	 */
	private synchronized void refreshRuntimeTimer() throws SQLException {
		String sql = "SELECT * FROM timer WHERE deleted = FALSE AND TIMESTAMPDIFF(SECOND, NOW(), COALESCE(snooze, due)) < ?";
		List<TimerInfo> timers = new ArrayList<>();
		
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, cfg.prefetchWindow / 1000);
			
			try (var rs = stmt.executeQuery()) {
				while (rs.next()) {
					timers.add(createTimerInfo(rs));
				}
			}
		}
		
		// convert new timers into runtime timers
		timers.removeIf(timerInfo -> runtimeTimer.containsKey(timerInfo.id));
		timers.forEach(this::queueTimer);
	}
	
	/**
	 * Instances the given TimerInfo into a Runtime Timer. Note that this method will not prevent queueing of already
	 * existing runtime timers.
	 *
	 * @param timerInfo The timer to instance.
	 */
	private synchronized void queueTimer(TimerInfo timerInfo) {
		var diff = Duration.between(Instant.now(), timerInfo.nextDue());
		var delay = Math.max(0, diff.toMillis());
		
		var task = timer.schedule(() -> {
			try {
				timerDue(timerInfo);
			} catch (ListenerException e) {
				log.error("error during finishing of timer: {}", timerInfo, e);
			}
		}, delay, TimeUnit.MILLISECONDS);
		
		
		runtimeTimer.put(timerInfo.id, new ScheduledTimer(timerInfo, task));
	}
	
	private TimerInfo createTimerInfo(ResultSet rs) throws SQLException {
		TimerInfo timerInfo = new TimerInfo();
		timerInfo.id = rs.getLong("id");
		
		timerInfo.service = rs.getString("service");
		timerInfo.user = rs.getString("user");
		timerInfo.channel = rs.getString("channel");
		
		// janky overflow shit can lead to unix epoch of 0 in DB which will cause jdbc to read "NULL" and lead to NPE, so we use unix_epoch of 1 instead
		timerInfo.creation = rs.getTimestamp("creation").toInstant();
		timerInfo.due = Objects.requireNonNullElse(rs.getTimestamp("due"), UNIX_EPOCH_SECOND_1).toInstant();
		
		var timestamp = rs.getTimestamp("snooze");
		if (timestamp != null)
			timerInfo.snooze = timestamp.toInstant();
		
		timerInfo.snoozeCount = rs.getInt("snoozeCount");
		
		timerInfo.text = rs.getString("text");
		timerInfo.deleted = rs.getBoolean("deleted");
		
		return timerInfo;
	}
	
	private Optional<Instant> parse(String arg, ZoneId zoneId) {
		var parser = new Parser(TimeZone.getTimeZone(zoneId));
		var parse = parser.parse(arg);
		var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
		
		if (!dates.isEmpty()) {
			return Optional.of(dates.get(0).toInstant());
		}
		return Optional.empty();
	}
	
	private static Optional<Pair<Instant, String>> shrinkingParse(String arg, ZoneId zoneId) {
		try {
			var parser = new Parser(TimeZone.getTimeZone(zoneId));
			
			// shorten the input string one word at a time and find largest matching string as date
			String[] w = arg.split(" ");
			for (int i = w.length; i >= 0; i--) {
				String part = String.join(" ", Arrays.copyOfRange(w, 0, i));
				List<DateGroup> parse = parser.parse(part);
				var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
				
				if (!parse.isEmpty() && parse.get(0).getText().equals(part)) {
					String message = String.join(" ", Arrays.copyOfRange(w, i, w.length));
					return Optional.of(Pair.of(dates.get(0).toInstant(), message));
				}
			}
			return Optional.empty();
		} catch (Throwable ignore) {
			// absolutely don't care about bugs in this library
			return Optional.empty();
		}
	}
	
	/**
	 * This method resolves a given timer string by either calling {@link #encodeTimer(long)} or looking up alias
	 * keywords.
	 *
	 * @param msg
	 * @return
	 * @throws SQLException
	 */
	private static String resolveTimerString(ListenerMessage msg) throws SQLException {
		// TODO: check constants for symbols
		throw new RuntimeException("implement me");
	}
	
	private static String encodeTimer(long l) {
		StringBuilder out = new StringBuilder((Long.SIZE / ENCODER_ALPHABET_LOG) + 1);
		while (l != 0) {
			int idx = (int) (l & ENCODER_BITMASK);
			l >>= ENCODER_ALPHABET_LOG;
			out.append(ENCODER_ALPHABET.charAt(idx));
		}
		return out.toString();
	}
	
	private static long decodeTimer(String s) throws IdParseException {
		s = s.toLowerCase();
		
		int out = 0;
		
		// max amount of steps until long is full or input is empty
		var limit = Math.min((Long.SIZE / ENCODER_ALPHABET_LOG) + 1, s.length());
		for (int i = 0; i < limit; i++) {
			char c = s.charAt(i);
			
			// look up index or fail if invalid
			long idx = ENCODER_ALPHABET.indexOf(c);
			if (idx < 0)
				throw new IdParseException();
			
			// move idx at correct position
			out |= (idx << (ENCODER_ALPHABET_LOG * i));
		}
		return out;
	}
	
	@ToString
	private static class TimerInfo {
		
		private long id;
		private String service;
		private String user;
		private String channel;
		private String text;
		
		private Instant creation = Instant.now();
		private Instant due;
		private Instant snooze;
		
		private int snoozeCount = 0;
		private boolean deleted = false;
		
		private Instant nextDue() {
			return Objects.requireNonNullElse(snooze, due);
		}
	}
	
	@AllArgsConstructor
	private static class ScheduledTimer {
		
		private final TimerInfo timerInfo;
		private final ScheduledFuture<?> future;
	}
	
	private static class Config {
		
		private @Positive long expire;
		private @Positive long prefetchWindow;
		private @Positive long prefetchInterval;
		private @Positive int abbrevLength;
	}
	
	private static class IdParseException extends Exception {
	}
}
