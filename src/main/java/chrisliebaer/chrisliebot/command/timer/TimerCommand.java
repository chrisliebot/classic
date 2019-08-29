package chrisliebaer.chrisliebot.command.timer;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: streamline code to reduce number of duplicated code paths and output handling
// TODO upgrade to v3
/* v3 features
 * upper limit for duration, number of timers (can be combined with scope based map used for other limits I can't remember)
 * permission for accessing other tasks / and deleting them
 */
@Slf4j
public class TimerCommand implements ChrislieListener.Command {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
	private static final Pattern FILENAME_PATTERN = Pattern.compile("^[0-9]+\\.json$");
	
	private File dir;
	private GsonValidator gson;
	
	private Map<Long, TimerDescription> timers = new HashMap<>();
	private Parser parser = new Parser();
	
	private Chrisliebot bot;
	private ContextResolver resolver;
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Vergiss nie wieder wann du deine Pizza aus dem Ofen holen musst: !timer 10 min Pizza rausholen. !timer list, !timer info|delete <id>");
	}
	
	@Override
	public synchronized void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		var cfg = gson.fromJson(json, Config.class);
		dir = new File(cfg.dir);
		
		if (!dir.isDirectory() || !dir.exists())
			if (!dir.mkdirs())
				throw new ListenerException("failed to create timer directory");
		
		if (!dir.canRead())
			throw new ListenerException("can't read timer directory");
	}
	
	@Override
	public synchronized void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		this.resolver = resolver;
		this.gson = bot.sharedResources().gson();
	}
	
	@Override
	public synchronized void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		// load tasks and start timer but don't recreate files
		if (!dir.exists())
			Preconditions.checkState(dir.mkdirs(), "failed to create timer task dir: " + dir);
		
		Preconditions.checkState(dir.isDirectory() && dir.canWrite(), "can't write to: " + dir);
		
		// recreate timers from config
		for (File file : dir.listFiles((d, name) -> FILENAME_PATTERN.asPredicate().test(name))) { // TODO find out if this warning is right
			try (FileReader fr = new FileReader(file)) {
				TimerDescription description = gson.fromJson(fr, TimerDescription.class);
				long id = Long.parseLong(file.getName().split("\\.")[0]);
				queueTimer(description, false, id);
			} catch (IOException e) {
				throw new ListenerException("failed to load timers from disk", e);
			}
		}
	}
	
	@Override
	public synchronized void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		// stop pending timer but keep files
		for (var v : timers.values()) {
			v.task().cancel();
		}
	}
	
	@Override
	public synchronized void execute(Invocation invc) throws ListenerException {
		var arg = invc.arg();
		var m = invc.msg();
		
		if (m.channel().isDirectMessage()) {
			ErrorOutputBuilder.generic("Timer sind aktuell nicht in privaten Nachrichten verfügbar.").write(invc).send(); // TODO: change that
			return;
		}
		
		if (arg.startsWith("info")) {
			var split = arg.split(" ", 2);
			if (split.length != 2) {
				ErrorOutputBuilder.generic("Du hast vergessen die Id des Timers anzugeben.").write(invc).send();
				return;
			}
			
			try {
				long id = Long.parseLong(split[1]);
				TimerDescription description = timers.get(id);
				if (description == null) {
					ErrorOutputBuilder.generic("Diese Id gibt es nicht.").write(invc).send();
					return;
				}
				
				createTimerReply(invc.reply().title("Information über Timer " + id), id, description).send();
			} catch (NumberFormatException e) {
				ErrorOutputBuilder.generic("Das ist keine Zahl.").write(invc).send();
			}
		} else if (arg.startsWith("list")) {
			var list = timers.entrySet().stream()
					.filter(e -> userOwnsTimer(m.user(), e.getValue()))
					.collect(Collectors.toList());
			
			var reply = invc.reply();
			reply.title("Timer von " + m.user().displayName());
			
			var desc = reply.description();
			var joiner = reply.convert().appendEscapeSub("${title}: ").joiner(", ");
			for (var e : list) {
				var id = e.getKey();
				var description = e.getValue();
				var abbrev = StringUtils.abbreviate(description.message(), 200);
				desc.appendEscape("Id " + id + ": ").appendEscape(abbrev).newLine().newLine();
				joiner.seperator().appendEscape("Id ").appendEscape(String.valueOf(id), ChrislieFormat.HIGHLIGHT).appendEscape(": " + abbrev);
			}
			
			if (list.isEmpty()) {
				desc.appendEscape("Du hast leider keine Timer");
				joiner.appendEscape("Du hast leider keine Timer");
			}
			
			reply.send();
		} else if (arg.startsWith("delete") || arg.startsWith("entfernen") || arg.startsWith("del")) {
			var split = arg.split(" ", 2);
			if (split.length != 2) {
				ErrorOutputBuilder.generic("Du hast keine Id angegeben.").write(invc).send();
				return;
			}
			
			try {
				long id = Long.parseLong(split[1]);
				TimerDescription description = timers.get(id);
				if (description == null) {
					ErrorOutputBuilder.generic("Diese Id gibt es nicht.").write(invc).send();
					return;
				}
				
				// check if user is allowed to delete this timer
				if (userOwnsTimer(m.user(), description)) {
					File timerFile = new File(dir, id + ".json");
					if (!timerFile.delete()) {
						ErrorOutputBuilder.generic("Da ging etwas schief.").write(invc).send();
						log.error("failed to delete timer file: {}", timerFile);
						return;
					}
					timers.remove(id);
					description.task().cancel();
					
					createTimerReply(invc.reply().title("Timer wurde gelöscht"), id, description).send();
				}
				
				
			} catch (NumberFormatException e) {
				ErrorOutputBuilder.generic("Das ist keine Zahl.").write(invc).send();
			}
		} else {
			Optional<Pair<Date, String>> result = shrinkingParse(arg);
			
			if (result.isEmpty()) {
				ErrorOutputBuilder.generic("Ich hab da leider keine Zeitangabe gefunden.").write(invc).send();
				return;
			}
			
			long timestamp = result.get().getLeft().getTime();
			long diff = timestamp - System.currentTimeMillis();
			String mesage = result.get().getRight();
			
			if (diff < 0) {
				ErrorOutputBuilder.generic("Für diesen Zeitpunkt brauchst du eine Zeitmaschine.").write(invc).send();
				return;
			}
			
			TimerDescription description = TimerDescription.builder()
					.service(m.service().identifier())
					.channel(m.channel().identifier())
					.nick(m.user().displayName())
					.identifier(m.user().identifier())
					.when(timestamp)
					.message(mesage)
					.build();
			
			long id;
			try {
				id = queueTimer(description, true);
			} catch (IOException e) {
				ErrorOutputBuilder.throwable(e).write(invc).send();
				log.warn("failed to write timer {} to disk", description, e);
				return;
			}
			
			var duration = C.durationToString(diff);
			var when = DATE_FORMAT.format(result.get().getLeft());
			
			var reply = invc.reply();
			reply.title("Timer gestellt");
			reply.description(description.message());
			reply.field("Dauer", duration);
			reply.field("Fällig", when);
			reply.field("Id", String.valueOf(id));
			
			reply.convert()
					.appendEscapeSub("${title} für: ")
					.appendEscapeSub("${description}", ChrislieFormat.HIGHLIGHT)
					.appendEscapeSub(", die Id lautet ${f-Id}, das ist in ")
					.appendEscapeSub("${f-Dauer}", ChrislieFormat.HIGHLIGHT)
					.appendEscapeSub(" also am ")
					.appendEscapeSub("${f-Fällig}", ChrislieFormat.HIGHLIGHT);
			
			reply.send();
		}
	}
	
	private static boolean userOwnsTimer(ChrislieUser user, TimerDescription description) {
		return user.identifier().equals(description.identifier());
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
				try {
					completeTimer(id); // TODO: this exception should be fed back into dispatcher, if possible
				} catch (ListenerException e) {
					log.warn("failed to complete timer with id {}", id, e);
				}
			}
		};
		description.task(task);
		bot.sharedResources().timer().schedule(task,
				Math.max(0, description.when() - System.currentTimeMillis()));
		
		return id;
	}
	
	private synchronized long getNewTimerId() {
		for (long i = 0; ; i++) {
			if (!timers.containsKey(i))
				return i;
		}
	}
	
	private synchronized void completeTimer(long id) throws ListenerException {
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
		
		var service = bot.service(description.service());
		if (service.isEmpty()) {
			log.warn("could not send timer to user since server is not available: {}", description);
			return; // TODO: requeue
		}
		
		var channel = service.get().channel(description.channel());
		if (channel.isEmpty()) {
			log.warn("could not print timer since not in channel: {}", description);
			return; // TODO: requeue since we otherwise lose timer if we are just starting
		}
		
		// locate user in channel for proper mention
		var chan = channel.get();
		List<ChrislieUser> accounts = chan.users().stream()
				.filter(u -> u.identifier().equals(description.identifier()))
				.collect(Collectors.toList());
		
		// fall back to nickname match if account is not in channel
		String mention = description.nick();
		if (!accounts.isEmpty())
			mention = accounts.stream()
					.map(ChrislieUser::mention)
					.collect(Collectors.joining(", "));
		
		var ctx = resolver.resolve(Selector::check, chan); // TODO: we also want the user that's part of the channel
		var ref = ctx.listener(this);
		
		var out = chan.output(LimiterConfig.of(ref.flexConf()));
		out.plain().append(mention);
		out.title("Es ist soweit");
		out.description(description.message());
		out.replace().append(mention).appendEscape(" es ist soweit: " + description.message());
		out.send();
	}
	
	private ChrislieOutput createTimerReply(ChrislieOutput reply, long id, TimerDescription description) {
		var maybeService = bot.service(description.service());
		if (maybeService.isEmpty()) {
			log.warn("service for timer completion not available");
			return reply; // TODO: this is a horrible hack
		}
		var service = maybeService.get();
		
		// process timer data
		var when = DATE_FORMAT.format(new Date(description.when()));
		var user = service.user(description.identifier());
		var nick = user.map(ChrislieUser::displayName).orElse(description.nick());
		
		reply.description(description.message());
		reply.field("Fällig", when);
		user.ifPresent(u -> reply.field("Besitzer", u.mention()));
		
		reply.convert()
				.appendEscapeSub("${title}, ")
				.appendEscape("Timer für ")
				.appendEscape(nick, ChrislieFormat.HIGHLIGHT)
				.appendEscape(": ").appendEscape(description.message(), ChrislieFormat.HIGHLIGHT)
				.appendEscape(", Fällig: ").appendEscape(when, ChrislieFormat.HIGHLIGHT);
		
		return reply;
	}
	
	@Data
	@Builder
	private static class TimerDescription {
		
		private @Positive long when;
		private @NotBlank String service;
		private @NotBlank String channel;
		private @NotBlank String nick;
		private @NotBlank String identifier;
		private String message;
		private transient TimerTask task;
		
	}
	
	private static class Config {
		
		private @NotBlank String dir;
	}
}
