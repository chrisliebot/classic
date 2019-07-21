package chrisliebaer.chrisliebot.config;

import com.google.gson.JsonElement;
import lombok.Data;

import java.util.*;

@Data
public class CommandConfig {
	private List<String> use;
	private transient CommandRegistry commandConfig = new CommandRegistry();
	
	@Data
	public static class CommandRegistry {
		
		private Set<String> unbind = new HashSet<>(0);
		private LinkedHashMap<String, CommandDefinition> cmdDef = new LinkedHashMap<>(0);
		private LinkedHashMap<String, List<String>> cmdBinding = new LinkedHashMap<>(0);
		private List<ListenerDefinition> listener = new ArrayList<>(0);
		
		public void merge(CommandRegistry o) {
			unbind.addAll(o.unbind);
			cmdDef.putAll(o.cmdDef);
			cmdBinding.putAll(o.cmdBinding);
			listener.addAll(o.listener);
		}
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
