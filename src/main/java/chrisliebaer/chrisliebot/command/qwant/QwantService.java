package chrisliebaer.chrisliebot.command.qwant;

import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

// https://github.com/NLDev/qwant-api/blob/master/DOCUMENTATION.md
// or just visit website and check network monitor
// some fields are skipped since they provide no use for this bot
public interface QwantService {
	
	public static final String BASE_URL = "https://api.qwant.com/";
	
	public static final String DEFAULT_LOCALE = "de_DE";
	
	public static final String TYPE_WEB = "web";
	public static final String TYPE_NEWS = "news";
	public static final String TYPE_IMAGES = "images";
	
	@AllArgsConstructor
	enum SafeSearch {
		OFF(0),
		MEDIUM(1),
		HIGH(2);
		private int code;
	}
	
	@AllArgsConstructor
	enum Type {
		WEB("web"),
		NEWS("news"),
		IMAGE("images");
		private String code;
	}
	
	@Deprecated // dont call directly
	@GET("v3/search/{type}?device=desktop&locale=" + DEFAULT_LOCALE)
	@SuppressWarnings({"MissingDeprecatedAnnotation"})
	public Call<QwantResponse> search(
			@Path("type") String type,
			@Query("t") String t,
			@Query("q") String query,
			@Query("safesearch") int safesearch,
			@Query("count") int count
	);
	
	public default Call<QwantResponse> search(String query, SafeSearch safesearch, int count, @NotNull Type type) {
		return search(type.code, type.code, query, safesearch.code, count);
	}
	
	@Data
	public static class QwantResponse {
		
		private String status;
		private int error; // only set if status is "error"
		private QwantData data;
		
		public List<QwantItem> items(GsonValidator gson, Type type) {
			return data.result.getItemsAndUnfuckMess(gson, type);
		}
		
		private static class QwantQuery {
			
			public String query;
		}
		
		private static class QwantData {
			
			public QwantResult result;
			public QwantQuery query;
		}
		
		public static class QwantMainlineItemsBullshit {
			
			public List<QwantResult> mainline;
		}
		
		private static class QwantResult {
			
			public JsonElement items;
			public String type;
			
			// web type search returns a bunch of bullshit typed json, so we fix that
			public List<QwantItem> getItemsAndUnfuckMess(GsonValidator gson, Type type) {
				if (items == null || items.isJsonNull()) {
					return List.of();
				}
				
				if (items.isJsonArray()) {
					return gson.fromJson(items, new TypeToken<List<QwantItem>>() {}.getType());
				}
				
				var mainline = gson.fromJson(items, QwantMainlineItemsBullshit.class);
				var list = new ArrayList<QwantItem>();
				for (var result : mainline.mainline) {
					if (result.type.equalsIgnoreCase(type.code)) {
						list.addAll(result.getItemsAndUnfuckMess(gson, type)); // will actually end up in first branch so no recursion
					}
				}
				return list;
			}
		}
		
		@Data
		public static class QwantItem {
			
			private String title;
			private JsonElement media;
			private String desc;
			private String url;
			
			public String mediaUrl(GsonValidator gson) {
				if (media == null || media.isJsonNull())
					return null;
				
				if (media.isJsonPrimitive()) {
					return media.getAsString();
				}
				
				if (media.isJsonArray()) {
					var array = media.getAsJsonArray();
					if (array.size() == 0) {
						return null;
					}
					
					var media = gson.fromJson(array.get(0), QwantMediaSubObject.class);
					return media.pict.url;
				}
				
				return null;
			}
		}
		
		public static class QwantMediaSubObject {
			public QwantMediaSubObject2 pict;
		}
		
		public static class QwantMediaSubObject2 {
			public String url;
		}
	}
}
