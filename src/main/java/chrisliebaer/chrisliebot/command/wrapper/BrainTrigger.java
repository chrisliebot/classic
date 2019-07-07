package chrisliebaer.chrisliebot.command.wrapper;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandContainer;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import chrisliebaer.chrisliebot.config.ConfigContext;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Remebers past executions and can be configured with a timeout or a chance to prevent execution of wrapped command.
 */
public class BrainTrigger implements CommandExecutor {
	
	private Config config;
	private HashMap<String, Long> lastExecutions = new HashMap<>(0);
	private CommandContainer cmd;
	
	public BrainTrigger(Config config, CommandContainer cmd) {
		this.config = config;
		this.cmd = cmd;
	}
	
	@Override
	public void execute(Message m, String arg) {
		var now = System.currentTimeMillis();
		var chance = ThreadLocalRandom.current().nextFloat();
		long lastExecution = lastExecutions.getOrDefault(m.source(), 0L);
		
		if (!(
				(config.mode == Config.Mode.AND && chance < config.chance && lastExecution + config.cooldown <= (now)) ||
						(config.mode == Config.Mode.OR && (chance < config.chance || lastExecution + config.cooldown <= (now)))
		)) {
			if (config.verbose) {
				m.reply(C.error("Dieser Befehlsaufruf wurde nicht gestattet."));
			}
			return;
		}
		
		cmd.execute(m, arg);
		lastExecutions.put(m.source(), now);
	}
	
	@Override
	public boolean requireAdmin() {
		return config.requireAdmin;
	}
	
	public static BrainTrigger fromJson(Gson gson, JsonElement json, ConfigContext.PreConfigAccessor configContext) {
		var config = gson.fromJson(json, Config.class);
		var cmd = configContext.getCommandByDefinition(config.cmdDef);
		return new BrainTrigger(config, cmd);
	}
	
	@Data
	private static class Config {
		
		private enum Mode {
			AND, OR
		}
		
		private long cooldown = 0;
		private float chance = 1f;
		private Mode mode;
		private boolean verbose;
		private boolean requireAdmin;
		private String cmdDef;
	}
}
