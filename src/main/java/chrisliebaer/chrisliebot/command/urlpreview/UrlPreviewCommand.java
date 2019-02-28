package chrisliebaer.chrisliebot.command.urlpreview;

import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;

public class UrlPreviewCommand implements CommandExecutor {
	
	private OkHttpClient client = SharedResources.INSTANCE().httpClient();
	private List<String> hostBlacklist;
	
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
	
	private void fetchLink(Message m, URL url) {
		// TODO: enhance with multiple handlers for different domains
		new GenericUrlPreview(url, m).start();
	}
	
	public static UrlPreviewCommand fromJson(Gson gson, JsonElement json) {
		var cfg = gson.fromJson(json, Config.class);
		return new UrlPreviewCommand(cfg.hostBlacklist());
	}
	
	@Data
	public static class Config {
		
		private List<String> hostBlacklist;
	}
}
