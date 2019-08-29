package chrisliebaer.chrisliebot.command.urbandictionary;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.urbandictionary.UrbanDictionaryService.DefinitionList;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UrbanDictionaryCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_NO_QUERY = ErrorOutputBuilder.generic("Du hast keinen Suchbegriff eingegeben.");
	private static final ErrorOutputBuilder ERROR_NO_MATCH = ErrorOutputBuilder.generic("Deine Suche ergab leider keine Treffer.");
	
	private UrbanDictionaryService service;
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(UrbanDictionaryService.BASE_URL)
				.client(bot.sharedResources().httpClient())
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(UrbanDictionaryService.class);
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var m = invc.msg();
		var reply = invc.reply();
		String term = invc.arg().trim();
		if (term.isEmpty()) {
			ERROR_NO_QUERY.write(reply).send();
			return;
		}
		
		var call = service.lookup(term);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<DefinitionList> call, Response<DefinitionList> response) {
				assert response.body() != null; // jesus shut up
				var defs = response.body().list();
				if (defs.isEmpty()) {
					ERROR_NO_MATCH.write(reply).send();
					return;
				}
				
				var def = defs.get(0);
				reply.title("Definition für " + def.word(), def.permalink());
				reply.description(def.definition());
				reply.author("Autor: " + def.author());
				reply.field("Beispiel", def.example());
				reply.footer("powered by urbandictionary.com");
				
				reply.send();
			}
			
			@Override
			public void onFailure(Call<DefinitionList> call, Throwable t) {
				ErrorOutputBuilder.remoteRequest(call.request(), t).write(reply).send();
			}
		});
	}
}
