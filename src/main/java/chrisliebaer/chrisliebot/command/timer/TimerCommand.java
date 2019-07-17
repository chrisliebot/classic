package chrisliebaer.chrisliebot.command.timer;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Slf4j
public class TimerCommand implements ChrisieCommand {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
	private static final Pattern FILENAME_PATTERN = Pattern.compile("^[0-9]+\\.json$");
	
	private ChrislieService service;
	private File dir;
	private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	
	private Map<Long, TimerDescription> timers = new HashMap<>();
	private Parser parser = new Parser();
	
	public TimerCommand(File dir) {
		this.dir = dir;
	}
	
	@Override
	public synchronized void execute(ChrislieMessage m, String arg) {
		if (m.channel().isDirectMessage()) {
			m.reply(C.error("Timer sind aktuell nur in Channeln verfügbar.")); // TODO: change that
			return;
		}
		
		if (arg.startsWith("info")) {
			var split = arg.split(" ", 2);
			if (split.length != 2) {
				m.reply(C.error("Du hast vergessen die Id des Timers anzugeben."));
				return;
			}
			
			try {
				long id = Long.parseLong(split[1]);
				TimerDescription description = timers.get(id);
				if (description == null) {
					m.reply(C.error("Diese Id gibt es nicht."));
					return;
				}
				
				m.reply("Timer " + id + ": " + C.highlight(description.message())
						+ ", Fällig: " + C.highlight(DATE_FORMAT.format(new Date(description.when()))));
			} catch (NumberFormatException e) {
				m.reply(C.error("Das ist keine Zahl."));
			}
		} else if (arg.startsWith("list")) {
			String list = timers.entrySet().stream()
					.filter(e -> userOwnsTimer(m.user(), e.getValue()))
					.map((Function<Map.Entry<Long, TimerDescription>, String>) in -> {
						long id = in.getKey();
						var description = in.getValue();
						var abbrev = StringUtils.abbreviate(description.message(), 15);
						return "Id " + id + ": " + abbrev;
					}).map(C::highlight)
					.collect(Collectors.joining(", "));
			m.reply("Deine Timer: " + list);
			
		} else if (arg.startsWith("delete") || arg.startsWith("entfernen") || arg.startsWith("del")) {
			var split = arg.split(" ", 2);
			if (split.length != 2) {
				m.reply(C.error("Du hast vergessen die Id des Timers anzugeben."));
				return;
			}
			
			try {
				long id = Long.parseLong(split[1]);
				TimerDescription description = timers.get(id);
				if (description == null) {
					m.reply(C.error("Diese Id gibt es nicht."));
					return;
				}
				
				// check if user is allowed to delete this timer
				if (m.user().isAdmin() || userOwnsTimer(m.user(), description)) {
					File timerFile = new File(dir, id + ".json");
					if (!timerFile.delete()) {
						m.reply(C.error("Da ging etwas schief."));
						log.error(C.LOG_PUBLIC, "failed to delete timer file: {}", timerFile);
						return;
					}
					timers.remove(id);
					description.task().cancel();
					m.reply("Der Timer '" + C.highlight(description.message()) + "' wurde gelöscht.");
				}
				
				
			} catch (NumberFormatException e) {
				m.reply(C.error("Das ist keine Zahl."));
			}
		} else {
			Optional<Pair<Date, String>> result = shrinkingParse(arg);
			
			if (result.isEmpty()) {
				m.reply(C.error("Ich hab da leider keine Zeitangabe gefunden."));
				return;
			}
			
			long timestamp = result.get().getLeft().getTime();
			long diff = timestamp - System.currentTimeMillis();
			String mesage = result.get().getRight();
			
			if (diff < 0) {
				m.reply(C.error("Für diesen Zeitpunkt brauchst du eine Zeitmaschine."));
				return;
			}
			
			TimerDescription description = TimerDescription.builder()
					.channel(m.channel().identifier())
					.nick(m.user().displayName())
					.softIdentifier(m.user().softIdentifer())
					.when(timestamp)
					.message(mesage)
					.build();
			
			long id;
			try {
				id = queueTimer(description, true);
			} catch (IOException e) {
				m.reply(C.error("Fehler beim Speichern des Timers."));
				log.warn(C.LOG_PUBLIC, "failed to write timer {} to disk", description, e);
				return;
			}
			
			m.reply("Timer gestellt für: " + C.highlight(DATE_FORMAT.format(result.get().getLeft())) + " die Id lautet " + id +
					" das ist in " + C.highlight(C.durationToString(diff)));
		}
	}
	
	private static boolean userOwnsTimer(ChrislieUser user, TimerDescription description) {
		return user.softIdentifer().equals(description.softIdentifier());
	}
	
	private synchronized Optional<Pair<Date, String>> shrinkingParse(String arg) {
		try {
			// shorten the input string one word at a time and find largest matching string as date
			String[] w = arg.split(" ");
			for (int i = w.length; i >= 0; i--) {
				String part = String.join(" ", Arrays.copyOfRange(w, 0, i));
				List<DateGroup> parse = parser.parse(part);
				var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
				
				if (!parse.isEmpty() && parse.get(0).getText().equals(part)) {
					String message = String.join(" ", Arrays.copyOfRange(w, i, w.length));
					return Optional.of(Pair.of(dates.get(0), message));
				}
			}
			return Optional.empty();
		} catch (Throwable ignore) {
			// absolutely don't care about bugs in this library
			return Optional.empty();
		}
	}
	
	@Override
	public synchronized void init(ChrislieService client) throws Exception {
		this.service = client;
		
		// load tasks and start timer but don't recreate files
		if (!dir.exists())
			Preconditions.checkState(dir.mkdirs(), "failed to create timer task dir: " + dir);
		
		Preconditions.checkState(dir.isDirectory() && dir.canWrite(), "can't write to: " + dir);
		
		// recreate timers from config
		for (File file : dir.listFiles((dir, name) -> FILENAME_PATTERN.asPredicate().test(name))) { // TODO find out if this warning is right
			try (FileReader fr = new FileReader(file)) {
				TimerDescription description = gson.fromJson(fr, TimerDescription.class);
				long id = Long.parseLong(file.getName().split("\\.")[0]);
				queueTimer(description, false, id);
			}
		}
	}
	
	@Override
	public synchronized void stop() throws Exception {
		// stop pending timer but keep files
		for (var v : timers.values()) {
			v.task().cancel();
		}
	}
	
	private synchronized long queueTimer(TimerDescription description, boolean writeToDisk) throws IOException {
		return queueTimer(description, writeToDisk, getNewTimerId());
	}
	
	private synchronized long queueTimer(TimerDescription description, boolean writeToDisk, long id) throws IOException {
		// write timer file to disk
		if (writeToDisk) {
			try (FileWriter fw = new FileWriter(new File(dir, id + ".json"))) {
				gson.toJson(description, fw);
			}
		}
		
		// after possible exception, create task and queue timer
		timers.put(id, description);
		var task = new TimerTask() {
			@Override
			public void run() {
				completeTimer(id);
			}
		};
		description.task(task);
		SharedResources.INSTANCE().timer().schedule(task,
				Math.max(0, description.when() - System.currentTimeMillis()));
		
		return id;
	}
	
	private synchronized long getNewTimerId() {
		for (long i = 0; ; i++) {
			if (!timers.containsKey(i))
				return i;
		}
	}
	
	private synchronized void completeTimer(long id) {
		TimerDescription description = timers.remove(id);
		
		/* If a timer is removed by a user, the queued task might already be running, in this case the
		 * timer list will no longer contain this timer, so in order to account for that, we need to check if
		 * the timer is actually in the map or we crash the shared timer thread.
		 */
		if (description == null) {
			return;
		}
		
		// remove timer file from disk
		File timerFile = new File(dir, id + ".json");
		if (!timerFile.delete()) {
			log.warn("failed to delete timer file {}", timerFile);
		}
		
		var channel = service.channel(description.channel());
		if (channel.isEmpty()) {
			log.warn(C.LOG_PUBLIC, "could not print timer since not in channel: {}", description);
			return; // TODO: requeue since we otherwise lose timer if we are just starting
		}
		
		// locate user in channel for proper mention
		var chan = channel.get();
		List<ChrislieUser> accounts = chan.users().stream()
				.filter(u -> u.softIdentifer().equals(description.softIdentifier()))
				.collect(Collectors.toList());
		
		// fall back to nickname match if account is not in channel
		String mention = description.nick();
		if (!accounts.isEmpty())
			mention = accounts.stream()
					.map(ChrislieUser::mention)
					.collect(Collectors.joining(", "));
		
		var out = chan.output();
		out.plain().append(mention).appendEscape(" es ist so weit: " + description.message());
		out.send();
	}
	
	public static TimerCommand fromJson(Gson gson, JsonElement element) {
		return new TimerCommand(new File(element.getAsString()));
	}
	
	@Data
	@Builder
	private static class TimerDescription {
		private long when;
		private String channel;
		private String nick;
		private String softIdentifier;
		private String message;
		private transient TimerTask task;
	
	}
}
