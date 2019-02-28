package chrisliebaer.chrisliebot.command.urbandictionary;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.apache.commons.text.StringSubstitutor;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface UrbanDictionaryService {

	public static final String BASE_URL = "https://api.urbandictionary.com/";
	
	@GET("v0/define")
	public Call<DefinitionList> lookup(@Query("term") String term);
	
	@Data
	public static class DefinitionList {
		private List<Definition> list;
	}
	
	@Data
	public static class Definition {
		private String definition, example, word;
		private String permalink;
		private String author;
		@SerializedName("thumbs_up") private int thumbsUp;
		@SerializedName("thumbs_down") private int thumbsdown;
	}
	
	public static String removeBrackets(String definition) {
		StringSubstitutor strSub = new StringSubstitutor(key -> key, "[", "]", '\\');
		return strSub.replace(definition);
	}
}
