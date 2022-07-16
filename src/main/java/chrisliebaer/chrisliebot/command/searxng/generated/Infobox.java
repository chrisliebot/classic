
package chrisliebaer.chrisliebot.command.searxng.generated;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Infobox {

    @SerializedName("infobox")
    @Expose
    public String infobox;
    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("content")
    @Expose
    public String content;
    @SerializedName("img_src")
    @Expose
    public String imgSrc;
    @SerializedName("attributes")
    @Expose
    public List<Attribute> attributes = null;
    @SerializedName("urls")
    @Expose
    public List<Url> urls = null;
    @SerializedName("relatedTopics")
    @Expose
    public List<RelatedTopic> relatedTopics = null;
    @SerializedName("engine")
    @Expose
    public String engine;
    @SerializedName("engines")
    @Expose
    public List<String> engines = null;

}
