package chrisliebaer.chrisliebot.command.memedb;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import chrisliebaer.chrisliebot.util.BetterScheduledService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemeDbCommand implements CommandExecutor {

	private Config cfg;

	private MemeDbService service;
	private BetterScheduledService updateService;

	// taggedMemes will be updated in a different thread than it is read.
	// Therefore if a map was set to this attribute it should not be modified
	// again. If you need to change or update the map, create a new instance and
	// overwrite the references after all changes were made.
	private Map<String, List<DatabaseEntry>> taggedMemes;

	public MemeDbCommand(Config cfg) {
		this.cfg = cfg;
		updateService = new BetterScheduledService(this::update,
				AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, cfg.updateInterval(), TimeUnit.MILLISECONDS));
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(cfg.baseUrl())
				.client(SharedResources.INSTANCE().httpClient())
				.addConverterFactory(GsonConverterFactory.create(SharedResources.INSTANCE().gson()))
				.build();
		service = retrofit.create(MemeDbService.class);
	}

	@Override
	public void execute(Message m, String arg) {
		if (taggedMemes == null) {
			m.reply(C.error("Ich habe leider aktuell keine Datenbank zum durchsuchen. Versuche es später nochmal."));
			return;
		}

		var query = arg.trim();
		if (query.isEmpty()) {
			m.reply(C.error("Du hast keine Suchanfrage eingegeben."));
			return;
		}

		var one = FuzzySearch.extractOne(query, taggedMemes.entrySet(), item -> item.getKey());


		if (one.getScore() <= cfg.acceptScore()) {
			m.reply(C.error("Ich habe leider keinen passenden Eintrag gefunden oder die Ergebnisse waren nicht gut genug."));
			return;
		}

		var items = one.getReferent().getValue();
		assert items.size() != 0;

		var choice = ThreadLocalRandom.current().nextInt(items.size());
		var item = items.get(choice);
		m.reply("Ergebnis: " + cfg.baseUrl() + "hash/" + item.hash() + " Score: " + one.getScore());
	}

	private void update() {
		try {
			Response<List<DatabaseEntry>> response = service.getDatabase().execute();
			if (!response.isSuccessful()) {
				log.warn(C.LOG_IRC, "request to meme database was not successfull, error code: {}", response.code());
				return;
			}

			var responseMemes = response.body();
			if (responseMemes == null) {
				log.warn(C.LOG_IRC, "received null from meme database server");
				return;
			}

			var newTaggedMemes = new HashMap<String, List<DatabaseEntry>>();
			for (var meme : responseMemes) {
				for (var tag : meme.tags()) {
					var memeList = newTaggedMemes.get(tag);
					if (memeList == null) {
						memeList = new ArrayList();
						newTaggedMemes.put(tag, memeList);
					}
					memeList.add(meme);
				}
			}
			this.taggedMemes = newTaggedMemes;
			log.debug("refreshed meme database, containt {} elements and {} distinct tags", responseMemes.size(), newTaggedMemes.size());

		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) {
			log.warn(C.LOG_IRC, "failed to fetch meme database", t);
		}
	}

	@Override
	public void start() throws Exception {
		updateService.startAsync().awaitRunning();
	}

	@Override
	public void stop() throws Exception {
		updateService.stopAsync().awaitTerminated();
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

	public static MemeDbCommand fromJson(Gson gson, JsonElement json) {
		return new MemeDbCommand(gson.fromJson(json, Config.class));
	}
}
