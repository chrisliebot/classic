package chrisliebaer.chrisliebot.command.haskell;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public class HaskellCommand implements ChrislieListener.Command {
	
	private static String FLEX_TIMEOUT = "haskell.timeout";
	
	private HaskellService service;
	private Config cfg;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(cfg.hostname())
				.client(bot.sharedResources().httpClient())
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(HaskellService.class);
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Führt die übergebene Haskell Expression aus. Funktionsdefinitionen können mit `let` in die Expression gebunden werden.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var flex = invc.ref().flexConf();
		var timeout = flex.getStringOrFail(FLEX_TIMEOUT);
		
		// build rpc call from arguments
		var param = HaskellService.Param.builder()
				.proc("mueval")
				.args(HaskellService.Args.builder().timelimit(timeout).expression(invc.arg()).build())
				.build();
		var call = service.runHaskell(param);
		
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<HaskellService.Output> call, Response<HaskellService.Output> response) {
				if (!response.isSuccessful()) {
					invc.exceptionHandler().unwrap(() -> ErrorOutputBuilder.remoteErrorCode(call.request(), response).write(invc).send());
					return;
				}
				var result = response.body();
				
				invc.exceptionHandler().unwrap(() -> {
					var reply = invc.reply();
					reply.title("Haskell ist Liebe");
					reply.thumbnail("https://chrisliebot.chrisliebaer.de/haskell_logo.png");
					reply.description(d -> d.appendEscape(result.output(), ChrislieFormat.BLOCK));
					
					if (result.returncode() != 0) {
						reply.footer("Das hat leider nicht geklappt");
						reply.markAsError();
					}
					
					reply.send();
				});
			}
			
			@Override
			public void onFailure(Call<HaskellService.Output> call, Throwable t) {
				invc.exceptionHandler().escalateException(new ListenerException("failed to contact haskell backend", t));
			}
		});
	}
	
	@Data
	private static class Config {
		
		@URL @NotNull
		private String hostname;
	}
}
