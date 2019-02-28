package chrisliebaer.chrisliebot.command.urlpreview;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.kitteh.irc.client.library.util.Format;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class GenericUrlPreview implements Callback {
	
	private static final int MAX_IRC_MESSAGE_LENGTH = 700;
	private static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
	private static final long PREVIEW_TIMEOUT = 10000; // cancel connection after 10 seconds even if we are still receiving data
	
	private OkHttpClient client = SharedResources.INSTANCE().httpClient();
	private Timer timer = SharedResources.INSTANCE().timer();
	
	private URL url;
	private Message m;
	
	@SneakyThrows
	public GenericUrlPreview(@NonNull URL url, @NonNull Message m) {
		this.url = url;
		this.m = m;
	}
	
	public void start() {
		var req = new Request.Builder().get()
				.url(url)
				.header("User-Agent", "Twitterbot/1.0") // otherwise we get blocked too often :(
				.build();
		var call = client.newCall(req);
		call.enqueue(this);
		
		// queue timer for cancelation
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				call.cancel();
				if (!call.isExecuted())
					log.debug(C.LOG_IRC, "canceled preview of {} since it took to long", url);
			}
		}, PREVIEW_TIMEOUT);
		
	}
	
	@Override
	public void onFailure(Call call, IOException e) {
		if (!e.getMessage().isEmpty())
			log.debug(C.LOG_IRC, "failed to connect to {}: {}", url, e.getMessage());
	}
	
	@Override
	public void onResponse(Call call, Response response) throws IOException {
		
		// check for mime type
		String contentType = response.header("Content-Type");
		if (contentType == null) {
			log.debug(C.LOG_IRC, "no content type provided: {}", url);
			return;
		}
		
		// we only care about html pages
		String mime = contentType.split(";")[0].trim();
		if (!"text/html".equalsIgnoreCase(mime)) {
			log.debug(C.LOG_IRC, "can't parse content type {} for {}", mime, url);
		}
		
		// documentation doesn't mention it, but we have to close the body
		try (response; ResponseBody cutBody = response.peekBody(MAX_CONTENT_LENGTH)) {
			Document doc = Jsoup.parse(cutBody.string());
			
			// try to get title first
			String summary = doc.title();
			
			// but prefer open graph
			Elements metaOgTitle = doc.select("meta[property=og:title]");
			if (metaOgTitle != null) {
				var ogTitle = metaOgTitle.attr("content");
				summary = ogTitle.isEmpty() ? summary : ogTitle;
			}
			
			// and try to also append open graph description
			Elements metaOgDescription = doc.select("meta[property=og:description]");
			if (metaOgDescription != null) {
				var ogDescription = metaOgDescription.attr("content");
				summary += ogDescription.isEmpty() ? "" : (" - " + ogDescription);
			}
			
			summary = summary
					.replaceAll("[\n\r\u0000]", "") // remove illegal irc characters
					.trim();
			
			// limit output to 500 characters at max
			if (summary.length() > MAX_IRC_MESSAGE_LENGTH)
				summary = summary.substring(0, MAX_IRC_MESSAGE_LENGTH).trim() + "[...]";
			
			if (!summary.isEmpty())
				m.reply(C.format("Linkvorschau: ", Format.BOLD) + summary);
		}
	}
}
