package chrisliebaer.chrisliebot.command.urlpreview;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UrlPreviewCommand implements CommandExecutor {
	
	private static final long URL_EXPIRE_TIME = 600000; // 10 minutes
	private static final int URL_MAX_HISTORY = 50; // remember no more than 50 urls
	
	private OkHttpClient client = SharedResources.INSTANCE().httpClient();
	private List<String> hostBlacklist;
	
	// This cache is used to track urls that are posted multiple times. It's basically a set.
	private Set<HistoryEntry> urlHistory = Collections.newSetFromMap(CacheBuilder.newBuilder()
			.expireAfterAccess(URL_EXPIRE_TIME, TimeUnit.MILLISECONDS)
			.maximumSize(URL_MAX_HISTORY)
			.<HistoryEntry, Boolean>build().asMap());
	
	// This cache is used to track the posted titles if website provides poor title and opengraph tags
	private Set<HistoryEntry> titleHistory = Collections.newSetFromMap(CacheBuilder.newBuilder()
			.expireAfterAccess(URL_EXPIRE_TIME, TimeUnit.MILLISECONDS)
			.maximumSize(URL_MAX_HISTORY)
			.<HistoryEntry, Boolean>build().asMap());
	
	public UrlPreviewCommand(List<String> hostBlacklist) {
		this.hostBlacklist = hostBlacklist;
	}
	
	@Override
	public void execute(Message m, String arg) {
		// don't reuse since not thread safe
		var extractor = LinkExtractor.builder()
				.linkTypes(EnumSet.of(LinkType.WWW, LinkType.URL))
				.build();
		
		for (LinkSpan link : extractor.extractLinks(m.message())) {
			try {
				URL url = new URL(m.message().substring(link.getBeginIndex(), link.getEndIndex()));
				if (!hostBlacklist.contains(url.getHost()))
					fetchLink(m, url);
			} catch (MalformedURLException ignore) {} // don't care about invalid links
		}
	}
	
	private synchronized void fetchLink(Message m, URL url) {
		HistoryEntry historyLookup = new HistoryEntry(url.toExternalForm(), m.source());
		
		// ignore url if seen recently in same channel
		if (urlHistory.contains(historyLookup)) {
			log.debug(C.LOG_IRC, "ignoring recently posted url: {} in {}", url, m.source());
			return;
		}
		
		// add url to history (wonky if lookup fails but who cares about shitty websites?)
		urlHistory.add(historyLookup);
		
		// TODO: enhance with multiple handlers for different domains
		new GenericUrlPreview(url, m, titleHistory).start();
	}
	
	public static UrlPreviewCommand fromJson(Gson gson, JsonElement json) {
		var cfg = gson.fromJson(json, Config.class);
		return new UrlPreviewCommand(cfg.hostBlacklist());
	}
	
	@Data
	private static class Config {
		
		private List<String> hostBlacklist;
	}
	
	@Data
	@EqualsAndHashCode
	@AllArgsConstructor
	@SuppressWarnings("PackageVisibleInnerClass")
	static class HistoryEntry {
		
		private @NonNull String data;
		private @NonNull String channelName;
	}
}
