package chrisliebaer.chrisliebot.command.reddit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RedditService {
	
	public static final String BASE_URL = "https://old.reddit.com/";
	
	@GET("r/{subreddit}/new.json?sort=new")
	public Call<SubredditListing> getFeed(
			@Path("subreddit") String subreddit
	);
}
