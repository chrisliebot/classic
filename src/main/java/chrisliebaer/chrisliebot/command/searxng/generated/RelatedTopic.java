
package chrisliebaer.chrisliebot.command.searxng.generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RelatedTopic {
	
	@SerializedName("name")
	@Expose
	public String name;
	@SerializedName("suggestions")
	@Expose
	public List<String> suggestions = null;
	
}
