package chrisliebaer.chrisliebot.command.searxng;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.SerializedOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.command.searxng.generated.Result;
import chrisliebaer.chrisliebot.command.searxng.generated.SearXNGResult;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.flex.CommonFlex;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.text.StringSubstitutor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings({"SynchronizeOnNonFinalField", "FieldAccessedSynchronizedAndUnsynchronized"})
// resultStorage never changes but can't be final
public class SearXNGSearchCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_NO_QUERY = ErrorOutputBuilder.generic("Du hast keine Suchanfrage eingegeben.");
	private static final ErrorOutputBuilder ERROR_NO_ACTIVE_SEARCH = ErrorOutputBuilder.generic("Es gibt keine aktive Suchanfrage.");
	private static final ErrorOutputBuilder ERROR_EOF = ErrorOutputBuilder.generic("Das waren alle Ergebnisse. Mehr hab ich nicht.");
	private static final ErrorOutputBuilder ERROR_NO_MATCH = ErrorOutputBuilder.generic("Deine Suche ergab leider keine Treffer.");
	
	private static final CommonFlex.Provider<SearXNGService.SafeSearch> FLEX_SAFE_SEARCH = CommonFlex.provider(
			(flex, key) -> flex.get(key, SearXNGService.SafeSearch.class), "searxng.safeSearch");
	
	private static final int RATE_LIMIT_CODE = 429;
	
	private ErrorOutputBuilder errorRateLimited;
	private ErrorOutputBuilder errorExternalBang;

	
	private SearXNGService service;
	private Chrisliebot bot;
	
	private Config cfg;
	private Cache<ChrislieIdentifier.ChannelIdentifier, List<Result>> resultStorage;
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Ich zeig dir das World Wide Web. Gib einfach deine Suchanfrage ein.");
	}
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		resultStorage = CacheBuilder.newBuilder()
				.expireAfterAccess(cfg.resultTimeout, TimeUnit.MILLISECONDS)
				.build();
		
		@SuppressWarnings("resource") // warning makes no sense and is probably result of lambda usage
				OkHttpClient client = bot.sharedResources().httpClient()
				.newBuilder().addNetworkInterceptor(
						c -> c.proceed(c.request()
								.newBuilder()
								.header("User-Agent", C.UA_CHROME).build())).build();
		
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(SearXNGService.BASE_URL)
				.client(client)
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(SearXNGService.class);
		
		errorRateLimited = ErrorOutputBuilder.generic(out -> out.appendEscape("Ich wurde ausgesperrt. Tja, kann man nix machen. Hau doch bitte @Chrisliebär von mir."));
		errorExternalBang = ErrorOutputBuilder.generic(out -> out.appendEscape("Tut mir leid. External Bangs (!!) supporte ich nicht."));
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var m = invc.msg();
		var query = invc.arg().trim();
		var identifier = ChrislieIdentifier.ChannelIdentifier.of(m.channel());
		var reply = invc.reply();
		
		if (query.isEmpty()) {
			ERROR_NO_QUERY.write(reply).send();
			return;
		}

		if (query.contains("!!")) {
			errorExternalBang.write(reply).send();
			return;
		}
		
		if ("next".equalsIgnoreCase(query)) {
			synchronized (resultStorage) {
				List<Result> pastResult = resultStorage.getIfPresent(identifier);
				if (pastResult == null) {
					ERROR_NO_ACTIVE_SEARCH.write(reply).send();
					return;
				}
				
				if (pastResult.isEmpty()) {
					ERROR_EOF.write(reply).send();
					resultStorage.invalidate(identifier);
					return;
				}
				
				printResultItem(reply, pastResult.remove(0));
				return;
			}
		}
		
		var safeSearch = FLEX_SAFE_SEARCH.getOrFail(invc);
		Call<SearXNGResult> call = service.search(query, safeSearch, cfg.type);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<SearXNGResult> c, Response<SearXNGResult> resp) {
				SearXNGResult body = resp.body();
				if (!resp.isSuccessful() || body == null) { // bad error code or "error" in status field of json
					switch(resp.code()) {
						case RATE_LIMIT_CODE -> errorRateLimited.write(reply).send();
						default -> {
							ErrorOutputBuilder.remoteErrorCode(c.request(), resp).write(reply).send();
							log.warn("remote host {} response code: {} ({})", c.request().url(), resp.code(), resp.message());
						}
					}

					// clear search cache, so we don't confuse user with old results
					synchronized (resultStorage) {
						resultStorage.invalidate(identifier);
					}
					
					return;
				}
				List<Result> items = body.results;
				if (items == null || items.isEmpty()) {
					ERROR_NO_MATCH.write(reply).send();
					return;
				}
				
				if (cfg.randomize)
					Collections.shuffle(items);
				else
					Collections.sort(items);
				
				synchronized (resultStorage) {
					// add result to storage for later lookups
					resultStorage.put(identifier, items);
					
					// pop and print
					printResultItem(reply, items.remove(0));
				}
			}
			
			@Override
			public void onFailure(Call<SearXNGResult> c, Throwable t) {
				ErrorOutputBuilder.remoteRequest(c.request(), t).write(reply).send();
			}
		});
	}
	
	private void printResultItem(ChrislieOutput reply, Result item) {

		StringSubstitutor strSub = new StringSubstitutor(key -> switch (key) {
			case "title" -> C.stripHtml(item.title);
			case "media" -> (item.imgSrc == null) ? null : (!item.imgSrc.startsWith("//")) ? item.imgSrc : "https:" + item.imgSrc;
			case "desc" -> C.stripHtml(item.content);
			case "url" -> item.prettyUrl;
			default -> key.toUpperCase();
		});
		
		cfg.output.apply(reply, strSub::replace);
		reply.send();
	}
	
	private static class Config {
		
		private @NotNull SerializedOutput output;
		private @NotNull SearXNGService.Type type;
		private @Positive long resultTimeout; // duration in milliseconds until cached results expire
		private boolean randomize;
	}
}
