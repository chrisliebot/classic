package chrisliebaer.chrisliebot.command.searxng;

import chrisliebaer.chrisliebot.command.searxng.generated.SearXNGResult;
import lombok.AllArgsConstructor;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface SearXNGService {
	
	String DEFAULT_LOCALE = "en";
	
	String TYPE_WEB = "web";
	String TYPE_NEWS = "news";
	String TYPE_IMAGES = "images";
	
	@AllArgsConstructor
	enum SafeSearch {
		OFF(0),
		ON(1);
		private final int code;
	}
	
	@AllArgsConstructor
	enum Type {
		WEB("general"),
		NEWS("news"),
		IMAGE("images");
		private final String code;
	}
	
	@Deprecated // dont call directly
	@GET("search?format=json&language=" + DEFAULT_LOCALE)
	@SuppressWarnings("MissingDeprecatedAnnotation")
	Call<SearXNGResult> search(
			@Query("categories") String categories,
			@Query("q") String query,
			@Query("safesearch") int safesearch,
			@Query("engines") String engines
	);
	
	default Call<SearXNGResult> search(String query, SafeSearch safesearch, @NotNull Type type, List<String> engines) {
		return search(type.code, query, safesearch.code, String.join(",", engines));
	}
}
