package chrisliebaer.chrisliebot.command.reddit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SubredditListing {
	
	private SubredditData data;
	
	@Data
	public static class SubredditData {
		private List<Container> children;
		private String before, after;
	}
	
	@Data
	public static class Container {
		private PostData data;
	}
	
	@Data
	public static class PostData {
		private String name; // what we need to stuff into "before" and "after"
		private String selftext;
		private String title;
		private String author;
		@SerializedName("ups") private int upvotes;
		@SerializedName("author_flair_text") private String authorFlairText;
		private String permalink; // missing reddit domain prefix
		@SerializedName("created_utc") private long createdUtc;
		private Preview preview;
	}
	
	@Data
	public static class Preview {
		private List<PreviewImages> images;
	}
	
	@Data
	public static class PreviewImages {
		private PreviewImagesUrl source;
	}
	
	@Data
	public static class PreviewImagesUrl {
		private String url;
	}
}
