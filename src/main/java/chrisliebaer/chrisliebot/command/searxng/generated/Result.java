
package chrisliebaer.chrisliebot.command.searxng.generated;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class Result implements Comparable<Result> {

    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("title")
    @Expose
    public String title;
    @SerializedName("content")
    @Expose
    public String content;
    @SerializedName("engine")
    @Expose
    public String engine;
    @SerializedName("parsed_url")
    @Expose
    public List<String> parsedUrl = null;
    @SerializedName("img_src")
    @Expose
    public String imgSrc = null;
    @SerializedName("thumbnail_src")
    @Expose
    public String thumbnailSrc = null;
    @SerializedName("template")
    @Expose
    public String template;
    @SerializedName("engines")
    @Expose
    public List<String> engines = null;
    @SerializedName("positions")
    @Expose
    public List<Integer> positions = null;
    @SerializedName("score")
    @Expose
    public Double score;
    @SerializedName("category")
    @Expose
    public String category;
    @SerializedName("pretty_url")
    @Expose
    public String prettyUrl;
    @SerializedName("open_group")
    @Expose
    public Boolean openGroup;
    @SerializedName("close_group")
    @Expose
    public Boolean closeGroup;


    @Override
    public int compareTo(@NotNull Result o) {
        return o.score.compareTo(this.score);
    }
}
