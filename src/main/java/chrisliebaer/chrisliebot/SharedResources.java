package chrisliebaer.chrisliebot;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.Gson;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.util.Timer;

public final class SharedResources extends AbstractIdleService {
	
	private static final String DEFAULT_USER_AGENT = "Chrisliebot/1.0 (+https://gitlab.com/Chrisliebaer/chrisliebot-irc)";
	
	@Getter private static final SharedResources INSTANCE = new SharedResources();
	
	private boolean init = false;
	
	@Getter private Gson gson;
	@Getter private OkHttpClient httpClient;
	@Getter private Timer timer;
	@Getter private DataSource dataSource;
	
	private SharedResources() {}
	
	public synchronized SharedResources init(String dataSource) {
		Preconditions.checkState(!init, "SharedResources can only be initialized once");
		init = true;
		this.dataSource = new MariaDbDataSource(dataSource);
		return this;
	}
	
	@Override
	protected synchronized void startUp() throws Exception {
		Preconditions.checkState(init, "SharedResources have not been initialized");
		
		gson = new Gson();
		httpClient = new OkHttpClient.Builder()
				.addNetworkInterceptor(c -> c.proceed(c.request().newBuilder().header("User-Agent", DEFAULT_USER_AGENT).build()))
				.build();
		timer = new Timer(true);
	}
	
	@Override
	protected synchronized void shutDown() throws Exception {
		timer.cancel();
		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
	}
}
