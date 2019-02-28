package chrisliebaer.chrisliebot.command.qwant;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import chrisliebaer.chrisliebot.command.qwant.QwantService.QwantResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Collections;

@Slf4j
public class QwantSearchCommand implements CommandExecutor {
	
	private Config cfg;
	private QwantService service;
	
	public QwantSearchCommand(@NonNull Config cfg) {
		this.cfg = cfg;
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(QwantService.BASE_URL)
				.client(SharedResources.INSTANCE().httpClient())
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		service = retrofit.create(QwantService.class);
	}
	
	@Override
	public void execute(Message m, String arg) {
		var query = arg.trim();
		
		if (query.isEmpty()) {
			m.reply(C.error("Du hast keine Suchanfrage eingegeben."));
			return;
		}
		
		Call<QwantResponse> call = service.search(query, cfg.safeSearch(), cfg.count(), cfg.type());
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<QwantResponse> call, Response<QwantResponse> resp) {
				QwantResponse body = resp.body();
				if (!resp.isSuccessful() || (body != null && !"success".equals(body.status()))) { // bad error code or "error" in status field of json
					m.reply("Remote Server meldet Fehlercode: " + resp.code() + " " + resp.message());
					log.warn("remote host {} response code: {} ({})", call.request().url(), resp.code(), resp.message());
					return;
				}
				assert body != null; // shut up about body being null, it can't
				var items = body.items();
				
				if (body.items().isEmpty()) {
					m.reply("Deine Suche ergab leider keine Treffer.");
					return;
				}
				
				if (cfg.randomize())
					Collections.shuffle(items);
				
				var item = items.get(0);
				StringSubstitutor strSub = new StringSubstitutor(key -> {
					switch (key) {
						case "title":
							return C.stripHtml(item.title());
						case "media":
							return item.media();
						case "desc":
							return C.stripHtml(item.desc());
						case "url":
							return item.url();
						default:
							return key.toUpperCase();
					}
				});
				m.reply(strSub.replace(cfg.output()));
			}
			
			@Override
			public void onFailure(Call<QwantResponse> call, Throwable t) {
				C.remoteConnectionError(call.request(), m, t);
			}
		});
	}
	
	public static QwantSearchCommand fromJson(Gson gson, JsonElement json) {
		return new QwantSearchCommand(gson.fromJson(json, Config.class));
	}
	
	@Data
	public static class Config {
		
		private String output;
		private int safeSearch = 0; // off, as god intended
		private String type; // web, news, images
		private boolean randomize;
		private int count; // limited to 50
	}
}
