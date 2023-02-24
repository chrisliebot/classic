package chrisliebaer.chrisliebot.command.mastodon;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.JsonElement;
import com.sys1yagi.mastodon4j.api.entity.Status;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;


@Slf4j
public class MastodonTimelineNotifier extends AbstractIdleService implements ChrislieListener {
	
	private static final int RETRY_SLEEP = 5000;
	private static final Pattern BIRD_MAKEUP_URL = Pattern.compile("https://bird\\.makeup/users/(?<username>[0-9A-Za-z_]+)/statuses/(?<tweetid>[0-9]+)");
	
	private Config cfg;
	private Chrisliebot bot;
	private ContextResolver resolver;
	
	private Thread thread;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		this.resolver = resolver;
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		startAsync().awaitRunning();
	}
	
	@Override
	public void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		stopAsync().awaitTerminated();
	}
	
	@Override
	protected void startUp() {
		thread = new Thread(this::run, "mastodon-timeline-notifier");
		thread.setDaemon(true);
		thread.start();
	}
	
	@Override
	protected void shutDown() throws Exception {
		thread.interrupt();
		thread.join();
	}
	
	private void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				startStream();
			} catch (IOException e) {
				log.warn("error during websocket connection, reconnecting", e);
				try {
					Thread.sleep(RETRY_SLEEP);
				} catch (InterruptedException ignore) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
	
	private void startStream() throws IOException {
		var http = bot.sharedResources().httpClient();
		var gson = bot.sharedResources().gson();
		
		var latch = new CountDownLatch(1);
		
		// check health of streaming api
		var healthRequest = new Request.Builder()
				.url("https://" + cfg.mastodonHost() + "/api/v1/streaming/health")
				.build();
		try (var healthResponse = http.newCall(healthRequest).execute()) {
			if (!healthResponse.isSuccessful())
				throw new IOException("streaming api health check failed: " + healthResponse);
		}
		
		var streamRequest = new Request.Builder()
				.url("wss://" + cfg.mastodonHost() + "/api/v1/streaming/?access_token=" + cfg.accessToken + "&stream=user")
				.build();
		var webSocket = http.newWebSocket(streamRequest, new WebSocketListener() {
			
			@Override
			public void onOpen(@NotNull WebSocket socket, @NotNull Response response) {
				log.info("websocket opened to {}", response.request().url());
			}
			
			@Override
			public void onMessage(@NotNull WebSocket socket, @NotNull String text) {
				if (text.isEmpty())
					return;
				
				log.trace("websocket message: {}", text);
				var msg = gson.fromJson(text, WebsocketMessage.class);
				
				if (msg.error != null) {
					log.warn("mastodon websocket error: {}", msg.error);
					socket.cancel();
					return;
				}
				
				if (!"update".equals(msg.event))
					return;
				
				var status = gson.fromJson(msg.payload, Status.class);
				var url = status.getUrl();
				if (url == null || url.isEmpty())
					return;
				
				for (var sub : cfg.subscriptions())
					if (sub.matches(status))
						postStatus(status, sub);
			}
			
			@Override
			public void onClosed(@NotNull WebSocket socket, int code, @NotNull String reason) {
				log.info("websocket closed: {} {}", code, reason);
				latch.countDown();
			}
			
			@Override
			public void onFailure(@NotNull WebSocket socket, @NotNull Throwable t, @Nullable Response response) {
				log.warn("websocket failed", t);
				latch.countDown();
			}
		});
		
		// wait for websocket to signal end
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.warn("websocket monitor interrupted, closing websocket and terminating websocket connection");
			webSocket.close(WebSocketCloseStatus.SERVICE_RESTART.code(), "endpoint shutting down");
			Thread.currentThread().interrupt();
		}
	}
	
	private void postStatus(Status status, Subscriptions sub) {
		var url = status.getUrl();
		
		var matchter = BIRD_MAKEUP_URL.matcher(url);
		if (matchter.matches()) {
			// this is a bird.makeup post, we need to resolve url to the actual tweet
			var username = matchter.group("username");
			var tweetId = matchter.group("tweetid");
			url = "https://twitter.com/" + username + "/status/" + tweetId;
		}
		
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
		
		try {
			// build output for message
			var out = maybeChannel.get().output(LimiterConfig.of(maybeRef.get().flexConf()));
			out.plain().append(url);
			out.send();
		} catch (ListenerException e) {
			log.warn("could not build output for channel {}", maybeChannel.get(), e);
		}
	}
	
	@Data
	private static class Config {
		
		@NotEmpty
		private String mastodonHost;
		
		@NotEmpty
		private String accessToken;
		
		private List<Subscriptions> subscriptions;
	}
	
	@Data
	private static class Subscriptions {
		
		private ChrislieIdentifier.ChannelIdentifier channel;
		private String handle;
		private boolean ignoreReblog;
		private boolean ignoreReply;
		
		public boolean matches(Status status) {
			if (ignoreReblog && status.getReblog() != null)
				return false;
			
			if (ignoreReply && (status.getInReplyToId() != null || status.getInReplyToAccountId() != null))
				return false;
			
			return handle.equals(status.getAccount().getAcct());
		}
	}
	
	private static class WebsocketMessage {
		
		private String error;
		private String event;
		private String payload;
	}
}
