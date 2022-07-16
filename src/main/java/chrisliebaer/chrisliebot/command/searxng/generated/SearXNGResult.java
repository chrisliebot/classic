
package chrisliebaer.chrisliebot.command.searxng.generated;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SearXNGResult {

    @SerializedName("query")
    @Expose
    public String query;
    @SerializedName("number_of_results")
    @Expose
    public Double numberOfResults;
    @SerializedName("results")
    @Expose
    public List<Result> results = null;
    @SerializedName("answers")
    @Expose
    public List<Object> answers = null;
    @SerializedName("corrections")
    @Expose
    public List<Object> corrections = null;
    @SerializedName("infoboxes")
    @Expose
    public List<Infobox> infoboxes = null;
    @SerializedName("suggestions")
    @Expose
    public List<String> suggestions = null;
    @SerializedName("unresponsive_engines")
    @Expose
    public List<Object> unresponsiveEngines = null;

}
