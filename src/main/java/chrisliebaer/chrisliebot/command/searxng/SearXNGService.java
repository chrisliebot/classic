package chrisliebaer.chrisliebot.command.searxng;

import chrisliebaer.chrisliebot.command.searxng.generated.Result;
import chrisliebaer.chrisliebot.command.searxng.generated.SearXNGResult;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
public interface SearXNGService {

	//TODO: Move to config
	String BASE_URL = "http://localhost:8080/";
	
	String DEFAULT_LOCALE = "en";
	
	String TYPE_WEB = "web";
	String TYPE_NEWS = "news";
	String TYPE_IMAGES = "images";
	
	@AllArgsConstructor
	enum SafeSearch {
		OFF(0),
		ON(1);
		private int code;
	}
	
	@AllArgsConstructor
	enum Type {
		WEB("general"),
		NEWS("news"),
		IMAGE("images");
		private String code;
	}
	
	@Deprecated // dont call directly
	@GET("search?format=json&language=" + DEFAULT_LOCALE)
	@SuppressWarnings({"MissingDeprecatedAnnotation"})
	Call<SearXNGResult> search(
			@Query("categories") String categories,
			@Query("q") String query,
			@Query("safesearch") int safesearch
			// @Query("count") int count
	);
	
	default Call<SearXNGResult> search(String query, SafeSearch safesearch, @NotNull Type type) {
		return search(type.code, query, safesearch.code);
	}
}
