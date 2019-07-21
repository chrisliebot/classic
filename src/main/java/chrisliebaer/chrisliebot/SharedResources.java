package chrisliebaer.chrisliebot;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import lombok.Getter;
import okhttp3.OkHttpClient;

import javax.sql.DataSource;
import java.util.Timer;

public final class SharedResources extends AbstractIdleService {
	
	private static final String DEFAULT_USER_AGENT = "Chrisliebot/1.0 (+https://gitlab.com/Chrisliebaer/chrisliebot-irc)";
	
	@Getter private static final SharedResources INSTANCE = new SharedResources();
	
	@Getter private Gson gson;
	@Getter private OkHttpClient httpClient;
	@Getter private Timer timer;
	@Getter private DataSource dataSource;
	
	private SharedResources() {}
	
	@Override
	protected void startUp() throws Exception {
		gson = new Gson();
		httpClient = new OkHttpClient.Builder()
				.addNetworkInterceptor(c -> c.proceed(c.request().newBuilder().header("User-Agent", DEFAULT_USER_AGENT).build()))
				.build();
		timer = new Timer(true);
	}
	
	@Override
	protected void shutDown() throws Exception {
		timer.cancel();
		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
	}
}
