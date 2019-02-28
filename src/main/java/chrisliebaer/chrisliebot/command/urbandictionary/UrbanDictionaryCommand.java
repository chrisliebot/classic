package chrisliebaer.chrisliebot.command.urbandictionary;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import chrisliebaer.chrisliebot.command.urbandictionary.UrbanDictionaryService.DefinitionList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UrbanDictionaryCommand implements CommandExecutor {
	
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
	public void execute(Message m, String arg) {
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
				C.remoteConnectionError(call.request(), m, t);
			}
		});
	}
	
	public static UrbanDictionaryCommand fromJson(Gson gson, JsonElement json) {
		return new UrbanDictionaryCommand();
	}
}
