package chrisliebaer.chrisliebot.command.timer;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.*;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.flex.CommonFlex;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.io.BaseEncoding;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* v3 features TODO
 * upper limit for duration, number of timers (can be combined with scope based map used for other limits I can't remember)
 * permission for accessing other tasks / and deleting them
 */
@Slf4j
public class TimerCommand implements ChrislieListener.Command { // TODO: move error message in static builders
	
	private static final BaseEncoding TIMER_CODEC = BaseEncoding.base64Url().omitPadding();
	private static final long PURGE_INTERVAL = 60 * 60 * 1000;
	
	private static final ErrorOutputBuilder ERROR_DATE_IN_PAST = ErrorOutputBuilder.generic("Dieser Zeitpunkt liegt in der Vergangenheit.");
	private static final ErrorOutputBuilder ERROR_INVALID_DATE = ErrorOutputBuilder.generic("Dieses Datum habe ich leider nicht verstanden.");
	private static final ErrorOutputBuilder ERROR_TIMER_NOT_DELETED = ErrorOutputBuilder.generic("Dieser Timer existiert noch.");
	
	private Config cfg;
	
	private Chrisliebot bot;
	private Timer timer;
	private DataSource dataSource;
	private ContextResolver resolver;
	
	private TimerTask purgeTask;
	private Map<Long, ScheduledTimer> timers = new HashMap<>();
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("!timer 10 min Pizza. !timer list, !timer info|delete|restore <id>, !timer snooze <id> 2 days");
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
		
		try {
			var timers = getAllTimers();
			timers.forEach(this::queueTimer);
			log.debug("loaded {} timers", timers.size());
		} catch (SQLException e) {
			throw new ListenerException("failed to load timers from database", e);
		}
		
		purgeTask = new TimerTask() {
			@Override
			public void run() {
				purgeExpired();
			}
		};
		timer.scheduleAtFixedRate(purgeTask, 0, PURGE_INTERVAL);
	}
	
	@Override
	public synchronized void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		timers.values().forEach(t -> t.timerTask.cancel());
		purgeTask.cancel();
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
		
		var pair = shrinkingParse(arg, CommonFlex.ZONE_ID().getOrFail(invc.ref().flexConf()));
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
		} catch (SQLException e) {
			throw new ListenerException("failed to store new timer in database", e);
		}
		
		queueTimer(timerInfo);
		
		var reply = invc.reply();
		reply.title("Neuer Timer angelegt");
		formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
		reply.send();
	}
	
	private synchronized void listCommand(Invocation invc) throws ListenerException {
		var timers = this.timers.values().stream()
				.map(t -> t.timerInfo)
				.filter(projectTimerList(invc.msg()))
				.collect(Collectors.toList());
		
		if (timers.isEmpty()) {
			ErrorOutputBuilder.generic("Du hast keine Timer, die ich dir hier anzeigen darf.").write(invc).send();
			return;
		}
		
		var reply = invc.reply();
		reply.title("Du hast hier zur Zeit " + timers.size() + " Timer");
		var joiner = reply.description().joiner(", ");
		
		for (var timer : timers) {
			var id = encodeTimer(timer.id);
			var abbrev = StringUtils.abbreviate(timer.text, cfg.abbrevLength);
			
			joiner.seperator().appendEscape(id, ChrislieFormat.BOLD).appendEscape(": ").appendEscape(abbrev);
		}
		reply.send();
	}
	
	private synchronized void deleteCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		
		var maybeTimer = getScheduleTimerFromArg(arg, invc);
		if (maybeTimer.isEmpty())
			return;
		var timer = maybeTimer.get();
		timer.timerTask.cancel();
		
		String sql = "UPDATE timer SET deleted = TRUE WHERE id = ?";
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, timer.timerInfo.id);
			stmt.execute();
		} catch (SQLException e) {
			throw new ListenerException("failed to mark timer as deleted", e);
		}
		
		var reply = invc.reply();
		reply.title("Timer gelöscht");
		formatTimerOutput(reply, timer.timerInfo, invc.ref().flexConf(), false);
		reply.send();
	}
	
	private synchronized void infoCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		
		var maybeTimer = getScheduleTimerFromArg(arg, invc);
		if (maybeTimer.isEmpty())
			return;
		var timer = maybeTimer.get();
		
		var reply = invc.reply();
		reply.title("Timerinformationen");
		formatTimerOutput(reply, timer.timerInfo, invc.ref().flexConf(), false);
		reply.send();
	}
	
	private synchronized void restoreCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		var id = decodeTimer(arg);
		
		try (var conn = dataSource.getConnection()) {
			var maybeTimerInfo = fetchFromDb(id, conn);
			if (maybeTimerInfo.isEmpty()) {
				ErrorOutputBuilder.generic("Diesen Timer gibt es nicht.").write(invc).send();
				return;
			}
			var timerInfo = maybeTimerInfo.get();
			
			if (!timerInfo.deleted) {
				ERROR_TIMER_NOT_DELETED.write(invc).send();
				return;
			}
			
			if (!projectTimerList(invc.msg()).test(timerInfo)) {
				ErrorOutputBuilder.generic("Du bist nicht berechtigt diesen Timer hier zu modifizieren.").write(invc).send();
				return;
			}
			
			if (timerInfo.nextDue().isBefore(Instant.now())) {
				ErrorOutputBuilder.generic("Dieser Timer ist abgelaufen und muss daher genoozed werden.").write(invc).send();
				return;
			}
			
			// update database record and requeue timer
			String sql = "UPDATE timer SET deleted = 1 WHERE id = ?";
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				stmt.executeUpdate();
			}
			
			// refetch and requeue
			timerInfo = fetchFromDb(id, conn).orElseThrow();
			queueTimer(timerInfo);
			
			var reply = invc.reply();
			reply.title("Timer wiederhergestellt.");
			formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
			reply.send();
			
		} catch (SQLException e) {
			throw new ListenerException("failed to restore timer", e);
		}
	}
	
	/**
	 * Takes a user provided timer identifier and attempts to return the matching timer. For the sake of simplicity, this method will take care of error messages.
	 *
	 * @param arg  The user provided timer id.
	 * @param invc The invocation that triggered this retrival operation. Required to perform permission checks.
	 * @return The requested timer or nothing of the user or context does not allow for retrival.
	 */
	private synchronized Optional<ScheduledTimer> getScheduleTimerFromArg(String arg, Invocation invc) throws ListenerException, IdParseException {
		if (arg.isBlank()) {
			ErrorOutputBuilder.generic("Du hast vergessen die Id des Timers anzugeben.").write(invc).send();
			return Optional.empty();
		}
		
		// attempt to convert encoded id to long
		var id = decodeTimer(arg);
		
		// check if timer exists
		var timer = timers.get(id);
		if (timer == null) {
			ErrorOutputBuilder.generic("Einen Timer mit dieser Id gibt es nicht.").write(invc).send();
			return Optional.empty();
		}
		
		// check if user and context is permitted to see timer
		if (!projectTimerList(invc.msg()).test(timer.timerInfo)) {
			ErrorOutputBuilder.generic("Diesen Timer darf ich hier nicht anzeigen").write(invc).send();
			return Optional.empty();
		}
		
		return Optional.of(timer);
	}
	
	private void formatTimerOutput(ChrislieOutput out, TimerInfo timerInfo, FlexConf flex, boolean due) throws ListenerException {
		var zoneId = CommonFlex.ZONE_ID().getOrFail(flex);
		var formater = CommonFlex.DATE_TIME_FORMAT().getOrFail(flex);
		
		var when = timerInfo.nextDue().atZone(zoneId).format(formater);
		var creation = timerInfo.creation.atZone(zoneId).format(formater);
		var id = encodeTimer(timerInfo.id);
		
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
		
		var convert = out.convert();
		if (due && user.isPresent()) {
			convert.append(user.orElseThrow().mention()).appendEscape(": ");
		}
		
		convert.appendEscapeSub("${title} - ")
				.appendEscape("Id: ").appendEscape(id)
				.appendEscape(", Besitzer: ")
				.appendEscape(user.map(ChrislieUser::mention).orElse("Unbekannt"), ChrislieFormat.HIGHLIGHT)
				.appendEscape(", Fällig: ").appendEscape(when, ChrislieFormat.HIGHLIGHT);
		
		if (timerInfo.snoozeCount > 0) {
			out.field("Snoozezähler", String.valueOf(timerInfo.snoozeCount));
			convert.appendEscape(", Snoozezähler: ").appendEscape(String.valueOf(timerInfo.snoozeCount));
		}
		
		convert.appendEscape(", Text: ").appendEscape(timerInfo.text);
	}
	
	private synchronized void snoozeCommand(Invocation invc) throws ListenerException, IdParseException {
		var arg = getSubCommandArg(invc.arg());
		
		var args = arg.split(" ", 2);
		if (args.length != 2) {
			ErrorOutputBuilder.generic("Du hast nicht angegeben, auf welchen Zeitpunkt ich den Timer verschieben soll.").write(invc).send();
			return;
		}
		var id = decodeTimer(args[0]);
		var maybeWhen = parse(args[1], CommonFlex.ZONE_ID().getOrFail(invc.ref().flexConf()));
		if (maybeWhen.isEmpty()) {
			ERROR_INVALID_DATE.write(invc).send();
			return;
		}
		var when = maybeWhen.get();
		
		if (when.isBefore(Instant.now())) {
			ERROR_DATE_IN_PAST.write(invc).send();
			return;
		}
		
		TimerInfo timerInfo;
		try {
			var maybeTimerInfo = snoozeTimer(id, when, projectTimerList(invc.msg()));
			if (maybeTimerInfo.isEmpty()) {
				ErrorOutputBuilder.generic("Diesen Timer gibt es nicht oder du bist nicht berechtigt ihn zu modifizieren.").write(invc).send();
				return;
			}
			timerInfo = maybeTimerInfo.get();
		} catch (SQLException e) {
			throw new ListenerException("failed to snooze timer", e);
		}
		
		var reply = invc.reply();
		reply.title("Der Timer wurde erfolgreich verschoben.");
		formatTimerOutput(reply, timerInfo, invc.ref().flexConf(), false);
		reply.send();
	}
	
	/**
	 * Removes the first word of the given string including any following space and returns whats left (if anything).
	 *
	 * @param arg The string to truncate.
	 * @return The remaining string. Never null.
	 */
	private static String getSubCommandArg(String arg) {
		var args = arg.split(" ", 2);
		if (args.length > 1)
			return args[1];
		return "";
	}
	
	private static Predicate<TimerInfo> projectTimerList(ChrislieMessage msg) {
		var user = msg.user();
		var channel = msg.channel();
		var guild = channel.guild();
		
		Predicate<TimerInfo> pred = timer -> timer.user.equals(user.identifier());
		
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
	 * Fetches the timer with the given id from the database and requeues it for the given instant.
	 *
	 * @param id        The id of the timer in the database.
	 * @param instant   The new instant at which the timer should be triggered.
	 * @param predicate An predicate that will be used to verify that the requested action is actually allowed on the timer.
	 * @return The updated TimerInfo or nothing, if the id does not belong to a known timer or the timer failed the predicate.
	 */
	private synchronized Optional<TimerInfo> snoozeTimer(long id, Instant instant, Predicate<TimerInfo> predicate) throws SQLException {
		
		try (var conn = dataSource.getConnection()) {
			var maybeTimer = fetchFromDb(id, conn);
			if (maybeTimer.isEmpty())
				return Optional.empty();
			var timer = maybeTimer.get();
			
			// verify permission to modify timer
			if (!predicate.test(timer))
				return Optional.empty();
			
			// update timer record in database
			String sql = "UPDATE timer SET snooze = ?, deleted = FALSE, snoozecount = snoozecount + 1 WHERE id = ?";
			
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.setTimestamp(1, Timestamp.from(instant));
				stmt.setLong(2, id);
				
				stmt.executeUpdate();
				
				// refetch updated timer
				timer = fetchFromDb(id, conn).orElseThrow();
				queueTimer(timer); // requeue
				return Optional.of(timer);
			}
		}
	}
	
	private synchronized Optional<TimerInfo> fetchFromDb(long id, Connection conn) throws SQLException {
		String sql = "SELECT * FROM timer WHERE id = ?";
		try (var stmt2 = conn.prepareStatement(sql)) {
			stmt2.setLong(1, id);
			
			try (var rs = stmt2.executeQuery()) {
				if (!rs.next())
					return Optional.empty();
				
				var timerInfo = createTimerInfo(rs);
				return Optional.of(timerInfo);
			}
		}
	}
	
	/**
	 * Called from timer thread to trigger timer output.
	 *
	 * @param timerInfo The stored timer information for information retrieval.
	 * @throws ListenerException If the bot framework is unable to initialize the output.
	 */
	private synchronized void timerDue(TimerInfo timerInfo) throws ListenerException {
		// remove timer instance from map, even if unsuccessfull, it needs to be removed
		timers.remove(timerInfo.id);
		
		var maybeChannel = bot.service(timerInfo.service)
				.flatMap(service -> service.channel(timerInfo.channel));
		var maybeUser = maybeChannel.flatMap(c -> c.user(timerInfo.user));
		var maybeRef = maybeChannel.flatMap(c -> resolver.resolve(Selector::check, c).listener(this));
		
		if (maybeChannel.isEmpty() || maybeUser.isEmpty() || maybeRef.isEmpty()) {
			log.warn("unable to open channel or user not in channel for timer {} snoozing by {}", timerInfo, cfg.snoozeOnExpire);
			try {
				snoozeTimer(timerInfo.id, Instant.now().plusMillis(cfg.snoozeOnExpire), t -> true);
			} catch (SQLException e) {
				log.error("failed to snooze failed timer: {}", timerInfo, e);
			}
			return;
		}
		
		var channel = maybeChannel.get();
		var ref = maybeRef.get();
		var out = channel.output(LimiterConfig.of(ref.flexConf()));
		
		out.title("Es ist soweit");
		out.plain().append(maybeUser.get().mention());
		formatTimerOutput(out, timerInfo, ref.flexConf(), true);
		out.send();
		
		// it might be possible that the task got deleted right before we entered this method, so we need to account for that when deleting from database
		String sql = "UPDATE timer SET deleted = TRUE WHERE id = ?";
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, timerInfo.id);
			stmt.execute();
		} catch (SQLException e) {
			log.error("failed to mark deletion for due timer: {}", timerInfo, e);
		}
	}
	
	private synchronized void purgeExpired() {
		String sql = "DELETE FROM timer " +
				"WHERE NOW() - COALESCE(snooze, due) > ?"; // we even delete non deleted ones after they reach out deadline, user might be gone
		
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, cfg.expire);
			
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
	 * @return A TimerInfo instance with it's database id set.
	 * @throws SQLException If an database error occurs.
	 */
	private synchronized TimerInfo createTimer(TimerInfo timerInfo) throws SQLException {
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
					return timerInfo;
				} else {
					throw new SQLException("failed to retrieve timer insert id");
				}
			}
		}
	}
	
	private synchronized Collection<TimerInfo> getAllTimers() throws SQLException {
		String sql = "SELECT * from timer WHERE deleted IS FALSE";
		List<TimerInfo> timers = new ArrayList<>();
		
		try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
			try (var rs = stmt.executeQuery()) {
				while (rs.next()) {
					timers.add(createTimerInfo(rs));
				}
			}
		}
		return timers;
	}
	
	private synchronized void queueTimer(TimerInfo timerInfo) {
		var diff = Duration.between(Instant.now(), timerInfo.nextDue());
		var delay = Math.max(0, diff.toMillis());
		
		var task = new TimerTask() {
			@Override
			public void run() {
				try {
					timerDue(timerInfo);
				} catch (ListenerException e) {
					log.error("error during timer expiration of timer: {}", timerInfo, e);
				}
			}
		};
		timer.schedule(task, delay);
		
		timers.put(timerInfo.id, new ScheduledTimer(timerInfo, task));
	}
	
	private static String encodeTimer(long l) {
		byte[] bytes = new byte[Long.BYTES];
		var bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.asLongBuffer().put(l);
		return TIMER_CODEC.encode(bytes);
	}
	
	private static long decodeTimer(String s) throws IdParseException {
		try {
			var bytes = TIMER_CODEC.decode(s);
			if (bytes.length != Long.BYTES)
				throw new IdParseException();
			var bb = ByteBuffer.wrap(bytes);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			return bb.asLongBuffer().get();
		} catch (IllegalArgumentException ignored) {
			throw new IdParseException();
		}
	}
	
	private TimerInfo createTimerInfo(ResultSet rs) throws SQLException {
		TimerInfo timerInfo = new TimerInfo();
		timerInfo.id = rs.getLong("id");
		
		timerInfo.service = rs.getString("service");
		timerInfo.user = rs.getString("user");
		timerInfo.channel = rs.getString("channel");
		
		timerInfo.creation = rs.getTimestamp("creation").toInstant();
		timerInfo.due = rs.getTimestamp("due").toInstant();
		
		var timestamp = rs.getTimestamp("snooze");
		if (timestamp != null)
			timerInfo.snooze = timestamp.toInstant();
		
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
	
	private static synchronized Optional<Pair<Instant, String>> shrinkingParse(String arg, ZoneId zoneId) {
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
		
		private TimerInfo timerInfo;
		private TimerTask timerTask;
	}
	
	private static class Config {
		
		private @Positive long expire;
		private @Positive long snoozeOnExpire;
		private @Positive int abbrevLength;
	}
	
	private static class IdParseException extends Exception {
	}
}
