package chrisliebaer.chrisliebot.protocol.irc;

import lombok.Data;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class IrcConfig {
	
	@Data
	public static class BotConfig {
		
		private boolean verbose;
		private boolean chatlog;
		private String prefix;
		private String logTarget;
		private String databasePool;
		private ConnectionConfig connection;
		private Set<String> ignore = new HashSet<>(0);
		private Set<String> admins = new HashSet<>(0);
	}
	
	@Data
	public static class ConnectionConfig {
		
		private String host;
		private Integer port;
		private String user; // for login, not nickname
		private String nickname;
		private String serverPassword;
		private Boolean secure; // enables tls
		private Integer flooding; // delay between messenges in ms
		private String realname;
	}
}
