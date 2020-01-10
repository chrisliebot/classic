package chrisliebaer.chrisliebot.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
public class VersionUtil {
	
	private static final String VERSION_FILE_PATH = "/vcs.properties";
	private static final String VERSION_UNKNOWN = "unknown";
	
	private static final VersionUtil INSTANCE = new VersionUtil();
	
	private final String version;
	private final String commit;
	private final String branch;
	private final String clean;
	private final String buildTime;
	
	public VersionUtil() {
		Properties properties = new Properties();
		try {
			try (var in = getClass().getResourceAsStream(VERSION_FILE_PATH)) {
				if (in != null)
					properties.load(in);
			}
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
			log.warn("failed to load version file from resources", e);
		}
		
		// get or load defaults
		version = properties.getProperty("version", VERSION_UNKNOWN);
		commit = properties.getProperty("commit", VERSION_UNKNOWN);
		branch = properties.getProperty("branch", VERSION_UNKNOWN);
		clean = properties.getProperty("clean", VERSION_UNKNOWN);
		buildTime = properties.getProperty("buildTime", VERSION_UNKNOWN);
	}
	
	public static String version() { return INSTANCE.version; }
	
	public static String commit() { return INSTANCE.commit; }
	
	public static String branch() { return INSTANCE.branch; }
	
	public static String clean() { return INSTANCE.clean; }
	
	public static String buildTime() { return INSTANCE.buildTime; }
	
	public static String shortVersion() {
		return INSTANCE.version + "@" + INSTANCE.buildTime + " (branch: " + INSTANCE.branch + ") clean: " + INSTANCE.clean;
	}
}

