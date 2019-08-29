package chrisliebaer.chrisliebot.command.mensa.api;

import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

import java.util.List;
import java.util.Map;

public interface MensaApiService {
	
	public static final String MENSA_BASE_URL = "https://www.sw-ka.de/json_interface/";
	
	@GET("general/")
	public Call<MensaApiMeta> getMeta(@Header("Authorization") String credentials);
	
	@GET("canteen/")
	public Call<JsonElement> getCanteen(@Header("Authorization") String credentials);
	
	/**
	 * Unfucks mensa API response.
	 *
	 * @param gson Instance used for parsing.
	 * @param json Regular API response.
	 * @return Not really unfucked but still better than what it was before and actually usable.
	 */
	public static Map<String, Map<Long, Map<String, List<MensaApiMeal>>>> unfuck(GsonValidator gson, JsonElement json) {
		if (json == null)
			return null;
		
		try {
			// the goal is to remove "date" and "import_date" fields that are completely out of place
			JsonObject object = json.getAsJsonObject();
			object.remove("date");
			object.remove("import_date");
			
			//noinspection EmptyClass
			return gson.fromJson(object, new TypeToken<Map<String, Map<Long, Map<String, List<MensaApiMeal>>>>>() {}.getType());
		} catch (IllegalStateException e) {
			throw new JsonParseException("json so broken, could not even unfuck it lol", e);
		}
	}
}
