package chrisliebaer.chrisliebot.command.reddit;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.BetterScheduledService;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import retrofit2.Retrofit;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedditListener implements ChrislieListener {
	
	private static final int REDDIT_COLOR = 16721664;
	
	private Config cfg;
	private long lastTimestamp;
	
	private Chrisliebot bot;
	private ContextResolver resolver;
	private RedditService service;
	
	private BetterScheduledService feedUpdate;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		this.resolver = resolver;
		
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(RedditService.BASE_URL)
				.client(bot.sharedResources().httpClient())
				.addConverterFactory(bot.sharedResources().gson().factory())
				.build();
		service = retrofit.create(RedditService.class);
		
		feedUpdate = new BetterScheduledService(this::poll,
				AbstractScheduledService.Scheduler.newFixedDelaySchedule(0, cfg.delay, TimeUnit.MILLISECONDS));
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		// initial request gets most recent posts timestamp
		try {
			fetch();
		} catch (IOException e) {
			throw new ListenerException("failed to fetch last timestamp", e);
		}
		
		feedUpdate.startAsync().awaitRunning();
	}
	
	@Override
	public void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		feedUpdate.stopAsync().awaitTerminated();
	}
	
	private void poll() {
		try {
			var feed = fetch();
			
			// prepare context for output generation
			var maybeChannel = cfg.channel.channel(bot);
			if (maybeChannel.isEmpty()) {
				log.warn("unable to resolve channel for subreddit: {}", cfg.subreddit);
				return;
			}
			
			var ctx = resolver.resolve(Selector::check, maybeChannel.get());
			var maybeRef = ctx.listener(this);
			if (maybeRef.isEmpty()) {
				log.warn("listener not present in channel for subreddit: {}", cfg.subreddit);
				return;
			}
			var ref = maybeRef.get();
			var limiterConf = LimiterConfig.of(ref.flexConf());
			
			// reverse so we post in correct order
			Collections.reverse(feed.data().children());
			
			for (var container : feed.data().children()) {
				var post = container.data();
				var out = maybeChannel.get().output(limiterConf);
				fillOutput(post, out);
				out.send();
			}
			
		} catch (Exception e) {
			log.warn("failed to feed feed for subreddit: {}", cfg.subreddit, e);
		}
	}
	
	private SubredditListing fetch() throws IOException {
		var call = service.getFeed(cfg.subreddit);
		
		var resp = call.execute();
		if (!resp.isSuccessful())
			throw new IOException("request failed: " + resp.code());
		
		var feed = resp.body();
		
		// remove older entries
		feed.data().children().removeIf(c -> c.data().createdUtc() <= lastTimestamp);
		
		// update last timestamp
		if (feed.data().children() != null) {
			for (var child : feed.data().children()) {
				lastTimestamp = Math.max(child.data().createdUtc(), lastTimestamp);
			}
		}
		log.trace("most recent timestamp for feed {}: {}", cfg.subreddit, lastTimestamp);
		
		return feed;
	}
	
	private void fillOutput(SubredditListing.PostData post, ChrislieOutput out) {
		
		if (post.author() != null) {
			var author = post.author();
			out.author(post.authorFlairText() != null ? "%s - %s".formatted(author, post.authorFlairText()) : author);
			out.authorUrl("https://www.reddit.com/user/" + author + "/");
		}
		
		if (post.title() == null)
			out.title("Zum Beitrag", "https://www.reddit.com" + post.permalink());
		else
			out.title(post.title(), "https://www.reddit.com" + post.permalink());
		
		if (post.preview() != null
				&& !post.preview().images().isEmpty()) {
			var preview = post.preview().images().get(0);
			var imgUrl = StringEscapeUtils.unescapeXml(preview.source().url());
			out.image(imgUrl);
		}
		
		if (post.selftext() != null && !post.selftext().isBlank()) {
			// reddit does very weird things and we simply try to unfuck it ¯\_(ツ)_/¯
			var sanitized = StringEscapeUtils.unescapeXml(post.selftext());
			sanitized = StringUtils.abbreviate(sanitized, MessageEmbed.TEXT_MAX_LENGTH);
			out.description().append(sanitized);
		}
		
		out.footer("r/" + cfg.subreddit);
		out.color(REDDIT_COLOR);
	}
	
	private static class Config {
		
		@NotNull private ChrislieIdentifier.ChannelIdentifier channel;
		@Positive private long delay;
		@NotEmpty private String subreddit;
	}
}
