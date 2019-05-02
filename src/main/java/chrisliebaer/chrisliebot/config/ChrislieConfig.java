package chrisliebaer.chrisliebot.config;

import com.google.gson.JsonElement;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChrislieConfig {
	
	@Data
	public static class BotConfig {
		
		private String prefix;
		private String logTarget;
		private String databasePool;
		private ConnectionConfig connection;
		private List<String> ignore;
		private List<String> admins;
	}
	
	@Data
	public static class CommandConfig {
		
		private Map<String, CommandDefinition> cmdDef;
		private Map<String, List<String>> cmdBinding;
		private List<ListenerDefinition> listener;
		private List<String> unbind;
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
	
	@Data
	public static class CommandDefinition {
		
		private String help;
		private String clazz;
		private JsonElement config;
		private boolean map = true; // set to true will automatically create command binding with same name
	}
	
	@Data
	public static class ListenerDefinition {
		
		private String clazz;
		private JsonElement config;
		private List<String> trigger;
	}
}
