package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.util.GsonValidator;
import chrisliebaer.chrisliebot.util.VersionUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
public class SharedResources extends AbstractIdleService {
	
	private static final String HTTP_PROXY_HOST = "HTTP_PROXY_HOST";
	private static final String HTTP_PROXY_PORT = "HTTP_PROXY_PORT";
	
	private static final String DEFAULT_USER_AGENT = "Chrisliebot/" + VersionUtil.version() + " (+https://github.com/chrisliebot)";
	
	@Getter private OkHttpClient httpClient;
	@Getter private ScheduledExecutorService timer;
	@Getter private GsonValidator gson;
	
	private MariaDbPoolDataSource dataSource;
	
	public SharedResources(@NonNull String dataSource, @NonNull GsonValidator gson) throws SQLException {
		this.dataSource = new MariaDbPoolDataSource(dataSource);
		this.gson = gson;
	}
	
	public DataSource dataSource() {
		return dataSource;
	}
	
	@SuppressWarnings("resource")
	@Override
	protected void startUp() throws Exception {
		// ping database to ensure basic functionality
		try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
			stmt.execute("SELECT 1");
		} catch (SQLException e) {
			throw new Exception("probe request to database failed", e);
		}
		
		var httpLogger = new HttpLoggingInterceptor(s -> {
			log.trace("HTTP REQUEST: {}", s);
		});
		httpLogger.setLevel(HttpLoggingInterceptor.Level.BASIC);
		
		
		var builder = new OkHttpClient.Builder()
				.addNetworkInterceptor(c -> c.proceed(c.request().newBuilder().header("User-Agent", DEFAULT_USER_AGENT).build()))
				.addNetworkInterceptor(httpLogger);
		
		// check for proxy in properties for debugging
		if (System.getProperty(HTTP_PROXY_HOST) != null || System.getProperty(HTTP_PROXY_PORT) != null) {
			try {
				var proxyHost = System.getProperty(HTTP_PROXY_HOST, "localhost");
				var proxyPort = System.getProperty(HTTP_PROXY_PORT, "8080");
				log.info("Using proxy via {}:{}", proxyHost, proxyPort);
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
				builder.proxy(proxy);
			} catch (NumberFormatException e) {
				log.error("invalid port for proxy", e);
			}
		}
		httpClient = builder.build();
		
		timer = new ScheduledThreadPoolExecutor(1, r -> {
			var t = new Thread(r, "SharedTimerExecutor");
			t.setDaemon(true);
			t.setUncaughtExceptionHandler((t1, e) -> log.error("uncaught exception in shared timer", e));
			return t;
		});
	}
	
	@Override
	protected void shutDown() throws Chrisliebot.ChrisliebotException {
		// remember: reverse order
		timer.shutdown();
		httpClient.dispatcher().executorService().shutdown(); // TODO: are the executors blocking? should we configure the pool by ourself?
		httpClient.connectionPool().evictAll();
		
		dataSource.close();
	}
}
