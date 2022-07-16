
package chrisliebaer.chrisliebot.command.searxng.generated;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RelatedTopic {

    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("suggestions")
    @Expose
    public List<String> suggestions = null;

}
