package chrisliebaer.chrisliebot.command.mensa.api;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Class representing meta data returned by mensa API.
 */
@Data
public class MensaApiMeta {
	
	private Map<String, Entry> mensa;
	@SerializedName("mensa_sort") private List<String> mensaSort;
	
	private long date;
	
	@Data
	public static class Entry {
		
		private String name;
		
		private Map<String, String> lines;
		@SerializedName("lines_sort") private List<String> linesSort;
	}
}
