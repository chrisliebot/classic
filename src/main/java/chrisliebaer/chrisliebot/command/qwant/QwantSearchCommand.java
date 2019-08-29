package chrisliebaer.chrisliebot.command.qwant;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.command.qwant.QwantService.QwantResponse;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.text.StringSubstitutor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
// TODO: implement SFW, NSFW filter via flexconf
public class QwantSearchCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_NO_QUERY = ErrorOutputBuilder.generic("Du hast keine Suchanfrage eingegeben.");
	private static final ErrorOutputBuilder ERROR_NO_ACTIVE_SEARCH = ErrorOutputBuilder.generic("Es gibt keine aktive Suchanfrage.");
	private static final ErrorOutputBuilder ERROR_EOF = ErrorOutputBuilder.generic("Das waren alle Ergebnisse. Mehr hab ich nicht.");
	private static final ErrorOutputBuilder ERROR_NO_MATCH = ErrorOutputBuilder.generic("Deine Suche ergab leider keine Treffer.");
	
	private static final int MAX_RESULT_STORAGE = 10;
	private static final int RATE_LIMIT_CODE = 429;
	
	private ErrorOutputBuilder errorRateLimited;
	
	private Config cfg;
	private QwantService service;
	
	private final Cache<String, List<QwantResponse.QwantItem>> resultStorage =
			CacheBuilder.newBuilder()
					.maximumSize(MAX_RESULT_STORAGE)
					.build();
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Ich zeig dir das World Wide Web.");
	}
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class); // TODO: validate config
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		OkHttpClient client = bot.sharedResources().httpClient()
				.newBuilder().addNetworkInterceptor(c -> c.proceed(c.request().newBuilder().header("User-Agent", C.UA_CHROME).build())).build(); // TODO check if this is true
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(QwantService.BASE_URL)
				.client(client)
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(QwantService.class);
		
		errorRateLimited = ErrorOutputBuilder.generic(out -> out.appendEscape("Ich wurde ausgesperrt. Bitte hilf mir: ").append(cfg.captchaUrl()));
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var m = invc.msg();
		var query = invc.arg().trim();
		var context = m.channel().identifier();
		var reply = invc.reply();
		
		if (query.isEmpty()) {
			ERROR_NO_QUERY.write(reply).send();
			return;
		}
		
		if ("next".equalsIgnoreCase(query)) {
			synchronized (resultStorage) {
				List<QwantResponse.QwantItem> pastResult = resultStorage.getIfPresent(context);
				if (pastResult == null) {
					ERROR_NO_ACTIVE_SEARCH.write(reply).send();
					return;
				}
				
				if (pastResult.isEmpty()) {
					ERROR_EOF.write(reply).send();
					resultStorage.invalidate(context);
					return;
				}
				
				printResultItem(reply, pastResult.remove(0));
				return; // don't forget to exit
			}
		}
		
		Call<QwantResponse> call = service.search(query, cfg.safeSearch(), cfg.count(), cfg.type());
		
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<QwantResponse> c, Response<QwantResponse> resp) {
				QwantResponse body = resp.body();
				if (!resp.isSuccessful() || (body != null && !"success".equals(body.status()))) { // bad error code or "error" in status field of json
					if (resp.code() == RATE_LIMIT_CODE) {
						errorRateLimited.write(reply).send();
					} else {
						ErrorOutputBuilder.remoteErrorCode(c.request(), resp).write(reply).send();
						log.warn("remote host {} response code: {} ({})", c.request().url(), resp.code(), resp.message());
					}
					
					// clear search cache so we don't confuse user with old results
					synchronized (resultStorage) {
						resultStorage.invalidate(context);
					}
					
					return;
				}
				assert body != null; // shut up about body being null, it can't
				List<QwantResponse.QwantItem> items = body.items();
				
				if (body.items().isEmpty()) {
					ERROR_NO_MATCH.write(reply).send();
					return;
				}
				
				if (cfg.randomize())
					Collections.shuffle(items);
				
				synchronized (resultStorage) {
					// add result to storage for later lookups
					resultStorage.put(context, items);
				}
				
				// pop and print
				printResultItem(reply, items.remove(0));
			}
			
			@Override
			public void onFailure(Call<QwantResponse> c, Throwable t) {
				ErrorOutputBuilder.remoteRequest(c.request(), t).write(reply).send();
			}
		});
	}
	
	private void printResultItem(ChrislieOutput reply, QwantResponse.QwantItem item) {
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
		
		reply.title(C.stripHtml(item.title()), item.url());
		reply.description(C.stripHtml(item.desc()));
		reply.image(item.media());
		reply.footer("powered by qwant.com", "https://www.qwant.com/favicon.png");
		
		reply.replace(strSub.replace(cfg.output()));
		
		reply.send();
	}
	
	@Data
	private static class Config {
		
		private String output;
		private int safeSearch = 0; // off, as god intended
		private String type; // web, news, images
		private boolean randomize;
		private int count; // limited to 50
		private String captchaUrl; // posted when rate limited
	}
}
