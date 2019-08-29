package chrisliebaer.chrisliebot.command.urlpreview;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
public class UrlPreviewCommand implements ChrislieListener {
	
	private static final String FLEX_LOGSIZE = "urlpreview.logsize";
	private static final String FLEX_EXPIRE_TIME = "urlpreview.expireTime";
	
	private static final long URL_EXPIRE_TIME = 600000; // 10 minutes
	private static final int URL_MAX_HISTORY = 50; // remember no more than 50 urls
	
	private Config cfg;
	
	/* TODO upgrade to v3 architecture
	 * host blacklist will become a regex
	 * create cache that can be used on channel basis (maybe consider creating container class for this case (even with scope: global, guild, channel, user, etc.)
	 *  and share with qwant?)
	 * have regex list with handlers that parse matching urls
	 * abstract common case where only extractor is needed (complicated cases might required rewriting the request to an api)
	 * don't care about output lenght since this will move into limiter config
	 */
	
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
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void onMessage(ListenerMessage msg, boolean isCommand) throws ListenerException {
		// don't reuse since not thread safe
		var extractor = LinkExtractor.builder()
				.linkTypes(EnumSet.of(LinkType.WWW, LinkType.URL))
				.build();
		
		var m = msg.msg().message();
		for (LinkSpan link : extractor.extractLinks(m)) {
			try {
				URL url = new URL(m.substring(link.getBeginIndex(), link.getEndIndex()));
				if (!cfg.hostBlacklist.contains(url.getHost()))
					fetchLink(msg, url);
			} catch (MalformedURLException ignore) {} // don't care about invalid links
		}
	}
	
	private synchronized void fetchLink(ListenerMessage m, URL url) {
		HistoryEntry historyLookup = new HistoryEntry(url.toExternalForm(), m.msg().channel().identifier());
		
		// ignore url if seen recently in same channel
		if (urlHistory.contains(historyLookup)) {
			log.debug("ignoring recently posted url: {} in {}", url, m.msg().channel().displayName());
			return;
		}
		
		// add url to history (wonky if lookup fails but who cares about shitty websites?)
		urlHistory.add(historyLookup);
		
		// TODO: enhance with multiple handlers for different domains
		new GenericUrlPreview(url, m, titleHistory).start();
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
