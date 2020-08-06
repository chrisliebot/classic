package chrisliebaer.chrisliebot.command.memedb;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.BetterScheduledService;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
// TODO: include nsfw filter, requires change to tags in meme indexer?
public class MemeDbCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_NO_DATABASE =
			ErrorOutputBuilder.generic("Ich habe leider aktuell keine Datenbank zum Durchsuchen. Versuche es später nochmal.");
	private static final ErrorOutputBuilder ERROR_NO_MATCH =
			ErrorOutputBuilder.generic("Ich habe leider keinen passenden Eintrag gefunden oder die Ergebnisse waren nicht gut genug.");
	
	private Config cfg;
	
	private MemeDbService service;
	private BetterScheduledService updateService;
	
	private Map<String, List<DatabaseEntry>> taggedMemes;
	
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Dank Memes vom Memelord. MEME HARD!! Du willst deine eigenen Memes teilen? Frag Chrisliebaer.");
	}
	
	// taggedMemes will be updated in a different thread than it is read.
	// Therefore if a map was set to this attribute it should not be modified
	// again. If you need to change or update the map, create a new instance and
	// overwrite the references after all changes were made.
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		updateService = new BetterScheduledService(this::update,
				AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, cfg.updateInterval(), TimeUnit.MILLISECONDS));
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(cfg.baseUrl())
				.client(bot.sharedResources().httpClient())
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(MemeDbService.class);
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		updateService.startAsync().awaitRunning();
	}
	
	@Override
	public void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		updateService.stopAsync().awaitTerminated();
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var m = invc.msg();
		if (taggedMemes == null) {
			ERROR_NO_DATABASE.write(invc).send();
			return;
		}
		
		var query = invc.arg().trim();
		if (query.isEmpty()) {
			var set = taggedMemes.values().stream()
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
			var idx = ThreadLocalRandom.current().nextInt(set.size());
			var it = set.iterator();
			for (int i = 0; i < idx; i++)
				it.next();
			printResult(invc.reply(), it.next());
			return;
		}
		
		var one = FuzzySearch.extractOne(query, taggedMemes.entrySet(), Map.Entry::getKey);
		
		if (one.getScore() <= cfg.acceptScore()) {
			ERROR_NO_MATCH.write(invc).send();
			return;
		}
		
		var items = one.getReferent().getValue();
		assert !items.isEmpty();
		
		var choice = ThreadLocalRandom.current().nextInt(items.size());
		var item = items.get(choice);
		printResult(invc.reply(), item);
	}
	
	private void printResult(ChrislieOutput reply, DatabaseEntry item) {
		var url = cfg.baseUrl() + "hash/" + item.hash();
		
		reply.title("Ergebnis", url);
		reply.image(url);
		
		reply.convert("${title}: ${titleUrl}");
		
		reply.send();
	}
	
	private void update() {
		try {
			Response<List<DatabaseEntry>> response = service.getDatabase().execute();
			if (!response.isSuccessful()) {
				log.warn("request to meme database was not successfull, error code: {}", response.code());
				return;
			}
			
			var responseMemes = response.body();
			if (responseMemes == null) {
				log.warn("received null from meme database server");
				return;
			}
			
			var newTaggedMemes = new HashMap<String, List<DatabaseEntry>>();
			for (var meme : responseMemes) {
				for (var tag : meme.tags()) {
					var memeList = newTaggedMemes.computeIfAbsent(tag, k -> new ArrayList<>());
					memeList.add(meme);
				}
			}
			this.taggedMemes = newTaggedMemes;
			log.debug("refreshed meme database, contains {} elements and {} distinct tags", responseMemes.size(), newTaggedMemes.size());
			
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) {
			log.warn("failed to fetch meme database", t);
		}
	}
	
	@Data
	private static class DatabaseEntry {
		
		private String hash;
		private String path;
		private String[] tags;
	}
	
	@Data
	private static class Config {
		
		private String baseUrl;
		private int updateInterval;
		private int acceptScore;
	}
	
	private interface MemeDbService {
		
		@GET("db.json")
		public Call<List<DatabaseEntry>> getDatabase();
	}
}
