package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.util.GsonValidator;
import chrisliebaer.chrisliebot.util.VersionUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import lombok.Getter;
import lombok.NonNull;
import okhttp3.OkHttpClient;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Timer;

public class SharedResources extends AbstractIdleService {
	
	private static final String DEFAULT_USER_AGENT = "Chrisliebot/" + VersionUtil.versionString() + " (+https://git.kd-tree.com/Chrisliebot/core)";
	
	@Getter private OkHttpClient httpClient;
	@Getter private Timer timer;
	@Getter private GsonValidator gson;
	
	private MariaDbPoolDataSource dataSource;
	
	public SharedResources(@NonNull String dataSource, @NonNull GsonValidator gson) {
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
		
		httpClient = new OkHttpClient.Builder()
				.addNetworkInterceptor(c -> c.proceed(c.request().newBuilder().header("User-Agent", DEFAULT_USER_AGENT).build()))
				.build();
		timer = new Timer(true);
	}
	
	@Override
	protected void shutDown() throws Exception {
		// remember: reverse order
		timer.cancel();
		httpClient.dispatcher().executorService().shutdown(); // TODO: are the executors blocking? should we configure the pool by ourself?
		httpClient.connectionPool().evictAll();
		
		dataSource.close();
	}
}
