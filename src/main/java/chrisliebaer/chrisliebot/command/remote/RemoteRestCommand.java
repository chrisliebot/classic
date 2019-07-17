package chrisliebaer.chrisliebot.command.remote;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Forwards a command invocation to a remote host.
 */
@Slf4j
public class RemoteRestCommand implements ChrisieCommand {
	
	private static final int MAX_RESPONSE_LENGHT = 4000;
	private static final int MAX_LINE_COUNT = 10;
	
	private final URL url;
	private Gson gson = SharedResources.INSTANCE().gson();
	private OkHttpClient client = SharedResources.INSTANCE().httpClient();
	
	public RemoteRestCommand(URL url) {
		this.url = url;
	}
	
	@Override
	public synchronized void execute(ChrislieMessage m, String arg) {
		
		// convert call to json
		var json = gson.toJson(RemoteMessageDto.of(m, arg));
		
		Request req = new Request.Builder()
				.url(url)
				.post(RequestBody.create(MediaType.get(C.MIME_TYPE_JSON), json))
				.build();
		client.newCall(req).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				ErrorOutputBuilder.remoteRequest(call.request(), e).write(m);
			}
			
			@Override
			public void onResponse(Call call, Response resp) throws IOException {
				if (!resp.isSuccessful()) {
					m.reply("Remote Server meldet Fehlercode: " + resp.code() + " " + resp.message());
					log.warn("remote host {} response code: {} ({})", url, resp.code(), resp.message());
					return;
				}
				
				try (resp; var body = resp.peekBody(MAX_RESPONSE_LENGHT)) {
					var lines = body.string().split("[\n\r\\u0000]", MAX_LINE_COUNT + 1);
					Arrays.stream(lines).forEachOrdered(m::reply);
				}
			}
		});
	}
	
	public static RemoteRestCommand fromJson(Gson gson, JsonElement json) throws MalformedURLException {
		var cfg = gson.fromJson(json, Config.class);
		return new RemoteRestCommand(new URL(cfg.url()));
	}
	
	@Data
	private static final class Config {
		private String url;
	}
}
