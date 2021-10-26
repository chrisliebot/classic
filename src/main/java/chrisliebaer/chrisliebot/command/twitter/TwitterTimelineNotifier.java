package chrisliebaer.chrisliebot.command.twitter;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;


// TODO: twitter api is a gigantic mess and we have no pollished library to use, so consider this feature an experiment
@Slf4j
public class TwitterTimelineNotifier implements ChrislieListener {
	
	private Config cfg;
	private Twitter twitter;
	
	private ScheduledFuture<?> pollTask;
	private boolean shutdown;
	
	private Chrisliebot bot;
	private ContextResolver resolver;
	private List<TimelineSubscription> subscriptions;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		this.resolver = resolver;
		var config = new ConfigurationBuilder()
				.setOAuthConsumerKey(cfg.apiKey)
				.setOAuthConsumerSecret(cfg.apiKeySecret)
				.setOAuthAccessToken(cfg.accessToken)
				.setOAuthAccessTokenSecret(cfg.accessTokenSecret)
				.build();
		twitter = new TwitterFactory(config).getInstance();
		
		// we need the account name and the last tweet id, luckily we can do this with a single request per subscrition
		subscriptions = new ArrayList<>(cfg.subscriptions.size());
		var partitions = Lists.partition(cfg.subscriptions, 100); // endpoints allows 100 users per request
		for (var part : partitions) {
			try {
				var ids = part.stream().map(TimelineSubscription::userId).mapToLong(v -> v).toArray();
				var users = twitter.users().lookupUsers(ids);
				
				// returned array may not match id order, so we need intermediate step
				var map = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
				
				// now match requested users to keys in map
				for (TimelineSubscription subscription : part) {
					var result = map.get(subscription.userId);
					if (result == null) {
						log.warn("unknown twitter account: {}", subscription);
						continue;
					}
					
					subscription.screenName = result.getScreenName();
					
					// user might not have a status or we are not allowed to access it
					var status = result.getStatus();
					subscription.lastId = status == null ? 0 : status.getId();
					
					// only store resolved users and ignore deleted ones
					subscriptions.add(subscription);
				}
			} catch (TwitterException e) {
				throw new ListenerException("failed to lookup subscribed twitter users", e);
			}
		}
		
		log.debug("will monitor the following subscriptions: {}", cfg.subscriptions);
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		var timer = bot.sharedResources().timer();
		pollTask = timer.scheduleWithFixedDelay(this::poll, 0, cfg.interval, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public synchronized void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		pollTask.cancel(false);
	}
	
	private synchronized void poll() {
		if (shutdown)
			return;
		
		// poll all subscribed timelines for new tweets
		for (var sub : subscriptions) {
			try {
				// twitter4j provides no return value for easy pagination but we don't care about more than 20 tweets
				// also: yes, we need to differentiate here because twitter4j is really broken
				var timeline = sub.lastId == 0 ? twitter.getUserTimeline(sub.userId) :
						twitter.getUserTimeline(sub.userId, new Paging().sinceId(sub.lastId));
				
				// newest entry is first, we don't want that, so we invert it
				Collections.reverse(timeline);
				
				postTweets(sub, timeline);
			} catch (TwitterException | ListenerException e) {
				log.warn("failed to get timeline for subscription {}", sub, e);
			}
		}
	}
	
	private void postTweets(TimelineSubscription sub, List<Status> tweets) throws ListenerException {
		if (tweets.isEmpty())
			return;
		
		// update last id and then post (ensure that we always update id even if output creates explosion
		sub.lastId = tweets.get(tweets.size() - 1).getId();
		
		var maybeChannel = sub.channel.channel(bot);
		if (maybeChannel.isEmpty()) {
			log.warn("could not find channel for subscription {}", sub);
			return;
		}
		
		var ctx = resolver.resolve(Selector::check, maybeChannel.get());
		var maybeRef = ctx.listener(this);
		if (maybeRef.isEmpty()) {
			log.warn("no listener ref found in channel for subscrition {}", sub);
			return;
		}
		
		for (var tweet : tweets) {
			var user = tweet.getUser();
			var out = maybeChannel.get().output(LimiterConfig.of(maybeRef.get().flexConf()));
			
			// might as well update the account name
			sub.screenName = user.getScreenName();
			
			if (!sub.includeRetweet && tweet.isRetweet())
				continue;
			
			if (!sub.includeReply && tweet.getInReplyToScreenName() != null)
				continue;
			
			// twitter4j doesn't provide perma link
			out.plain()
					.append("https://twitter.com/")
					.append(user.getScreenName())
					.append("/status/")
					.append(String.valueOf(tweet.getId()));
			out.send();
		}
	}
	
	@Data
	private static class Config {
		
		public long interval;
		private String apiKey, apiKeySecret;
		private String accessToken, accessTokenSecret;
		
		private List<TimelineSubscription> subscriptions;
	}
	
	@Data
	private static class TimelineSubscription {
		
		private ChrislieIdentifier.ChannelIdentifier channel;
		private long userId;
		private boolean includeRetweet;
		private boolean includeReply;
		
		private transient String screenName; // also called @handle
		private transient long lastId = -1;
	}
}
