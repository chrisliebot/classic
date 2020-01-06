package chrisliebaer.chrisliebot.command.mensa;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.command.mensa.api.MensaApiMeal;
import chrisliebaer.chrisliebot.command.mensa.api.MensaApiMeta;
import chrisliebaer.chrisliebot.command.mensa.api.MensaApiService;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.BetterScheduledService;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MensaCommand implements ChrislieListener.Command {
	
	// symbols to prefix various kinds of meals
	private static final String FLEX_FISH_CODE = "mensa.symbol.fish";
	private static final String FLEX_MEAT_CODE = "mensa.symbol.meat";
	private static final String FLEX_VEG_CODE = "mensa.symbol.veg";
	
	private static final String FLEX_DEFAULT_MENSA = "mensa.default"; // default mensa if no mensa is given
	private static final String FLEX_FILTER = "mensa.filter"; // array with lines in form of "$mensa.$line|*" to filter
	
	private static final String FLEX_CUTOFF = "mensa.cutoff"; // price below which meals are not displayed
	private static final String FLEX_ALLOW_LIST = "mensa.allowList"; // enables or disables `list` subdommand
	private static final String FLEX_ALLOW_PICK = "mensa.allowPick"; // enables or disables picking a mensa by name
	
	private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.00");
	
	private Config cfg;
	
	private Chrisliebot bot;
	private BetterScheduledService updateService;
	private MensaApiService service;
	
	private Map<String, Mensa> menu = new HashMap<>();
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Stellt den aktuellen Speiseplan des Studierendenwerks bereit.");
	}
	
	@Override
	public synchronized void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		this.cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public synchronized void init(Chrisliebot bot, ContextResolver resolver) {
		this.bot = bot;
		updateService = new BetterScheduledService(this::update,
				AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, cfg.updateInterval, TimeUnit.MILLISECONDS));
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(MensaApiService.MENSA_BASE_URL)
				.client(bot.sharedResources().httpClient())
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(MensaApiService.class);
	}
	
	@Override
	public synchronized void start(Chrisliebot bot, ContextResolver resolver) {
		updateService.startAsync().awaitRunning();
	}
	
	@Override
	public synchronized void stop(Chrisliebot bot, ContextResolver resolver) {
		updateService.stopAsync().awaitTerminated();
	}
	
	@Override
	public synchronized void execute(Invocation invc) throws ListenerException {
		var m = invc.msg();
		var arg = invc.arg();
		var flex = invc.ref().flexConf();
		var dateFormat = new SimpleDateFormat("EE dd.MM.yyyy"); // not threadsafe
		
		boolean useDisplay = true;
		
		// required since Arrays.asList doesn't support remove
		var args = new ArrayList<>(Arrays.asList(arg.split(" ")));
		
		// check for raw name flag and remove from argument stack
		var it = args.iterator();
		while (it.hasNext()) {
			if ("-r".equals(it.next())) {
				useDisplay = false;
				it.remove();
			}
		}
		
		// the parameters to gather for invocation
		long timestamp = System.currentTimeMillis();
		String mensaName = flex.getStringOrFail(FLEX_DEFAULT_MENSA);
		
		if (!args.isEmpty() && !args.get(0).isBlank()) { // argument stack is not empty
			if ("list".equalsIgnoreCase(args.get(0))) { // check if first argument matches "list"
				
				// list requires permission
				if (!flex.isSet(FLEX_ALLOW_LIST))
					return;
				
				var reply = invc.reply();
				reply.title("Diese Mensen kenne ich");
				var joiner = reply.description().joiner(", ");
				for (String s : menu.keySet()) {
					joiner.seperator().appendEscape(s, ChrislieFormat.HIGHLIGHT);
				}
				reply.send();
				return;
			} else {
				var argMensaName = args.get(0).toLowerCase();
				
				// mensa selection is only attempted if user is allowed to
				if (flex.isSet(FLEX_ALLOW_PICK)) {
					if (menu.containsKey(argMensaName.toLowerCase())) { // check if first argument matches valid mensa name
						mensaName = argMensaName;
						
						// remove mensa name from argument stack
						args.remove(0);
					} else if (args.size() >= 2) { // if two arguments, assume user mistyped mensa name
						ErrorOutputBuilder.generic(out -> out
								.appendEscape("Ich kenne keine Mensa mit dem Namen ").appendEscape(argMensaName, ChrislieFormat.HIGHLIGHT)).write(invc).send();
						return;
					}
				}
				
				// check if time offset is present
				if (!args.isEmpty()) {
					timestamp = parseDayOffset(args.get(0));
				}
			}
		}
		
		// negative timestamp indicates invalid input
		if (timestamp < 0) {
			ErrorOutputBuilder.generic("Ich habe leider keine Ahnung welcher Tag das sein soll.").write(invc).send();
			return;
		}
		
		// uptime timestamp to start of day
		timestamp = timestampToDate(timestamp);
		long finalTimestamp = timestamp;
		
		// get mensa and find next matching day
		Mensa mensa = menu.get(mensaName);
		
		if (mensa == null) {
			ErrorOutputBuilder.generic("Ich habe keine Daten für diese Mensa.").write(invc).send();
			return;
		}
		
		var maybeDay = mensa.records().stream().dropWhile(in -> in.timestamp() < finalTimestamp).findFirst();
		
		if (maybeDay.isEmpty()) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Ich habe leider keine Daten ab dem ").appendEscape(dateFormat.format(new Date(finalTimestamp)))).write(invc).send();
			return;
		}
		var day = maybeDay.get();
		
		// output code starts here
		var reply = invc.reply();
		var replace = reply.replace();
		reply.title("Mensaeinheitsbrei für " + (useDisplay ? mensa.displayName() : mensa.name()) + " am " + dateFormat.format(new Date(day.timestamp())));
		replace
				.appendEscape("Mensaeinheitsbrei für ")
				.appendEscape(useDisplay ? mensa.displayName() : mensa.name(), ChrislieFormat.HIGHLIGHT)
				.appendEscape(" am ")
				.appendEscape(dateFormat.format(new Date(day.timestamp())), ChrislieFormat.HIGHLIGHT)
				.newLine();
		
		// load symbols used to mark food
		var symbolFish = flex.getString(FLEX_FISH_CODE).orElse("");
		var symbolmeat = flex.getString(FLEX_MEAT_CODE).orElse("");
		var symbolVeg = flex.getString(FLEX_VEG_CODE).orElse("");
		
		// load ignore list
		Set<String> ignoreLines;
		//noinspection UnnecessaryCodeBlock
		{ // scope fuckery to hide type fuckery, the perfect crime
			Optional<Set<String>> maybeIgnoreLines = flex.get(FLEX_FILTER, new TypeToken<Set<String>>() {}.getType());
			ignoreLines = maybeIgnoreLines.orElse(Set.of());
		}
		
		// load cuttoff price
		var cutoff = flex.getDoubleorFail(FLEX_CUTOFF);
		
		for (MensaLine line : day.lines()) {
			// skip lines on ignore list
			if (ignoreLines.contains(mensa.name + "." + line.name))
				continue;
			
			String lineName = useDisplay ? line.displayName() : line.name();
			
			// build meals of line
			StringJoiner joiner = new StringJoiner(", ").setEmptyValue("");
			for (MensaApiMeal meal : line.meals()) {
				// highlight meal type with unicode
				boolean meat = meal.cow() || meal.cowRaw() || meal.pork() || meal.porkRaw();
				boolean veg = meal.veg() || meal.vegan();
				boolean fish = meal.fish();
				String mealSymbol = meat ? symbolmeat : (veg ? symbolVeg : (fish ? symbolFish : ""));
				
				// skip meals below cutoff
				if (meal.price1().doubleValue() < cutoff)
					continue;
				
				String price = PRICE_FORMAT.format(meal.price1());
				
				// append dish with whitespace if set
				String mealTitle = meal.meal() + (meal.dish() == null || meal.dish().isBlank() ? "" : (" " + meal.dish()));
				
				// accumulate meals of current line
				joiner.add(mealSymbol + mealTitle + " (" + price + "€)");
			}
			
			var lineStr = joiner.toString();
			
			// filtering may produce empty lines, so we need to check and skip them during output
			if (!lineStr.isBlank()) {
				reply.field(lineName, lineStr);
				replace.appendEscape(lineName, ChrislieFormat.BOLD).appendEscape(": " + lineStr).newLine();
			}
		}
		
		reply.send();
	}
	
	private static long parseDayOffset(String s) {
		// java sucks. attempt to parse as long or assume day of week
		try {
			return dayOffsetToDate(System.currentTimeMillis(), Integer.parseInt(s));
		} catch (NumberFormatException igored) {
			// assume day of week
			return C.stringToDay(s).map(in -> dayOffsetToDate(System.currentTimeMillis(), in)).orElse(-1L);
		}
	}
	
	/**
	 * Converts the given timestamp into a timestamp of the same day but at 0 o'clock. That is exactle when the day began.
	 *
	 * @param timestamp Timestamp of arbitrary day.
	 * @return Timestamp of beginning of same day.
	 */
	public static long timestampToDate(long timestamp) {
		Calendar cal = Calendar.getInstance(); // mensa is actually using local time, so we have to as well
		cal.setTimeInMillis(timestamp);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	/**
	 * Takes an offset in days and an timestamp and adjusts the timestamp by the given offset.
	 *
	 * @param timestamp The starting point.
	 * @param offset    The offset in days.
	 * @return The starting timestamp but shifted by the given offset.
	 */
	public static long dayOffsetToDate(long timestamp, int offset) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestampToDate(timestamp));
		cal.add(Calendar.DATE, offset);
		return cal.getTimeInMillis();
	}
	
	/**
	 * Takes a given timestamp and shifts it to the next or same day of the week that's given.
	 *
	 * @param timestamp The starting point.
	 * @param dayOfWeek The day of the week that the given timestamp should be
	 * @return The timestamp of the next day, starting from the given timestamp.
	 */
	public static long dayOffsetToDate(long timestamp, DayOfWeek dayOfWeek) {
		LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
		date = date.with(TemporalAdjusters.nextOrSame(dayOfWeek));
		return date.atStartOfDay().atZone(TimeZone.getDefault().toZoneId()).toEpochSecond() * 1000;
	}
	
	private synchronized void update() {
		try {
			String credentials = Credentials.basic(cfg.username, cfg.password);
			Call<MensaApiMeta> metaReq = service.getMeta(credentials);
			Call<JsonElement> canteensReq = service.getCanteen(credentials);
			
			// no enqueue since we absolutely need to block for result
			Response<MensaApiMeta> metaRes = metaReq.execute();
			if (!metaRes.isSuccessful()) {
				log.warn("mensa update failed at meta request: {}", metaRes);
				return;
			}
			
			Response<JsonElement> canteenRes = canteensReq.execute();
			if (!canteenRes.isSuccessful()) {
				log.warn("mensa update failed at canteen request: {}", canteenRes);
				return;
			}
			
			MensaApiMeta meta = metaRes.body();
			JsonElement canteenJson = canteenRes.body();
			
			// some sanity checks
			if (meta == null || canteenJson == null) {
				log.warn("mensa returned null json");
				return;
			}
			
			// reads as: mensaname, timestamp, line name, meals
			Map<String, Map<Long, Map<String, List<MensaApiMeal>>>> canteen = MensaApiService.unfuck(bot.sharedResources().gson(), canteenJson);
			
			// clean up returned json and attempt to validate structure
			menu = validateAndFilter(meta, canteen);
			
			log.trace("updated mensa menu: {}", menu);
			
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) { // when dealing with mensa, we don't fuck around
			log.warn("failed to update mensa data", t);
		}
	}
	
	// welcome to generic hell java 11 edition (also the most defensive method you will ever find)
	private synchronized Map<String, Mensa> validateAndFilter(MensaApiMeta meta, Map<String, Map<Long, Map<String, List<MensaApiMeal>>>> canteen) {
		Preconditions.checkArgument(meta.mensa() != null, "no mensa entries in meta");
		Preconditions.checkArgument(canteen != null, "canteens is null");
		
		// loop all mensas
		Map<String, Mensa> mensaMenu = new HashMap<>(canteen.size());
		for (Map.Entry<String, Map<Long, Map<String, List<MensaApiMeal>>>> mensaEntry : canteen.entrySet()) {
			Preconditions.checkArgument(mensaEntry.getValue() != null, "mensa has null day list");
			
			String mensaName = mensaEntry.getKey();
			MensaApiMeta.Entry mensaMeta = meta.mensa().get(mensaName);
			
			Preconditions.checkArgument(mensaMeta != null, "no meta for mensa " + mensaName);
			Preconditions.checkArgument(mensaMeta.name() != null && !mensaMeta.name().isEmpty(),
					"mensa meta does not contain display name");
			Preconditions.checkArgument(mensaMeta.lines() != null, "lines null in meta of mensa " + mensaName);
			
			String mensaDisplayName = mensaMeta.name();
			
			// loop all days of mensa
			Mensa mensa = new Mensa(mensaName, mensaDisplayName, new ArrayList<>(mensaEntry.getValue().size()));
			for (Map.Entry<Long, Map<String, List<MensaApiMeal>>> dayEntry : mensaEntry.getValue().entrySet()) {
				Preconditions.checkArgument(dayEntry.getKey() != null, "timestamp is null in mensa " + mensaName);
				
				long timestamp = dayEntry.getKey() * 1000; // convert to miliseconds
				
				Preconditions.checkArgument(dayEntry.getValue() != null,
						"line is null in mensa " + mensaName + "@" + timestamp);
				
				Map<String, List<MensaApiMeal>> lines = dayEntry.getValue();
				
				// ensure each line has record in meta
				for (String lineName : lines.keySet()) {
					Preconditions.checkArgument(mensaMeta.lines().containsKey(lineName),
							"line " + lineName + " without meta in mensa " + mensaName + "@" + timestamp);
					Preconditions.checkArgument(mensaMeta.linesSort().contains(lineName),
							"line " + lineName + " without sort meta in mensa " + mensaName + "@" + timestamp);
				}
				
				// iterate over lines in order of meta
				MensaRecord mensaRecord = new MensaRecord(timestamp, new ArrayList<>(lines.size()));
				for (String lineName : mensaMeta.linesSort()) {
					
					List<MensaApiMeal> line = lines.get(lineName);
					String lineDisplayName = mensaMeta.lines().get(lineName);
					
					// QUIRK: remove meals if invalid
					line.removeIf(in -> in.meal() == null);
					
					// sort in descending order by price
					line.sort((o1, o2) -> o2.price1().subtract(o1.price2()).signum());
					
					// add line if not empty (by this point)
					if (!line.isEmpty())
						mensaRecord.lines().add(new MensaLine(lineName, lineDisplayName, line));
				}
				
				// add day if at least one line is present
				if (!mensaRecord.lines().isEmpty())
					mensa.records().add(mensaRecord);
			}
			
			// sort days by timestamp
			Collections.sort(mensa.records());
			
			// finally add mensa to menu, delicious
			mensaMenu.put(mensaName, mensa);
		}
		
		return mensaMenu;
	}
	
	@Data
	@AllArgsConstructor
	private static class Mensa {
		
		private String name, displayName;
		private List<MensaRecord> records;
	}
	
	@Data
	@AllArgsConstructor
	private static class MensaRecord implements Comparable<MensaRecord> {
		
		private long timestamp; // beginning of the day, this record is valid for
		private List<MensaLine> lines; // ordered by meta
		
		@Override
		public int compareTo(MensaRecord o) {
			return Long.signum(timestamp - o.timestamp);
		}
	}
	
	@Data
	@AllArgsConstructor
	private static class MensaLine {
		
		private String name, displayName;
		private List<MensaApiMeal> meals;
	}
	
	private static class Config {
		
		private @NotNull @NotBlank String username;
		private @NotNull @NotBlank String password;
		private @Positive long updateInterval;
	}
}
