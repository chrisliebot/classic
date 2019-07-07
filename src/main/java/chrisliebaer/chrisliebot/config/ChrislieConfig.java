package chrisliebaer.chrisliebot.config;

import com.google.gson.JsonElement;
import lombok.Data;

import java.util.*;

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
	public static class CommandRegistry {
		private List<String> unbind;
		private List<String> use;
		
		private transient CommandConfig commandConfig = new CommandConfig();
	}
	
	@Data
	public static class CommandConfig {
		
		private LinkedHashMap<String, CommandDefinition> cmdDef = new LinkedHashMap<>();
		private LinkedHashMap<String, List<String>> cmdBinding = new LinkedHashMap<>();
		private List<ListenerDefinition> listener = new ArrayList<>();
		
		public void merge(CommandConfig o) {
			cmdDef.putAll(o.cmdDef);
			cmdBinding.putAll(o.cmdBinding);
			listener.addAll(o.listener);
		}
	}
	
	@Data
	public static class ConnectionConfig {
		
		private boolean verbose;
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
