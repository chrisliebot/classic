package chrisliebaer.chrisliebot.command.urbandictionary;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.command.urbandictionary.UrbanDictionaryService.DefinitionList;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UrbanDictionaryCommand implements ChrisieCommand {
	
	private UrbanDictionaryService service;
	
	public UrbanDictionaryCommand() {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(UrbanDictionaryService.BASE_URL)
				.client(SharedResources.INSTANCE().httpClient())
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		service = retrofit.create(UrbanDictionaryService.class);
	}
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		String term = arg.trim();
		if (term.isEmpty()) {
			m.reply(C.error("Du hast keinen Suchbegriff eingegeben."));
			return;
		}
		
		var call = service.lookup(term);
		call.enqueue(new Callback<>() {
			@Override
			public void onResponse(Call<DefinitionList> call, Response<DefinitionList> response) {
				assert response.body() != null; // jesus shut up
				var defs = response.body().list();
				if (defs.isEmpty()) {
					m.reply("Deine Suche ergab leider keiner Treffer.");
					return;
				}
				
				var def = defs.get(0);
				m.reply("Beste Definition für " + C.highlight(def.word()) + ": "
						+ UrbanDictionaryService.removeBrackets(def.definition()));
			}
			
			@Override
			public void onFailure(Call<DefinitionList> call, Throwable t) {
				ErrorOutputBuilder.remoteRequest(call.request(), t).write(m);
			}
		});
	}
}
