package chrisliebaer.chrisliebot.command.simplecommand;


import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class SimpleCommand implements CommandExecutor {
	
	private SimpleCommandOutput output;
	
	public SimpleCommand(SimpleCommandOutput output) {
		this.output = output;
	}
	
	@Override
	public void execute(Message m, String arg) {
		var out = output.out(arg);
		
		if (out != null && !out.isEmpty())
			m.reply(out);
	}
	
	public static CommandExecutor fromJson(Gson gson, JsonElement json) throws IOException {
		return new SimpleCommand(simpleCommandFromJson(gson, json));
	}
	
	private static SimpleCommandOutput simpleCommandFromJson(Gson gson, JsonElement json) throws IOException {
		var cfg = gson.fromJson(json, SimpleCommandConfig.class);
		
		switch (cfg.type()) {
			case "static":
				return SimpleCommandOutput.staticCommandOutput(cfg.config().getAsString());
			case "ctcpActionWrapper":
				return SimpleCommandOutput.ctcpActionWrapper(simpleCommandFromJson(gson, cfg.config()));
			case "mapper":
				HashMap<String, SimpleCommandOutput> map = new HashMap<>();
				for (var entry : cfg.config().getAsJsonObject().entrySet())
					map.put(entry.getKey(), simpleCommandFromJson(gson, entry.getValue()));
				return SimpleCommandMappedOutput.fromMap(map);
			case "mapperEscape":
				return SimpleCommandMappedOutput.escape(simpleCommandFromJson(gson, cfg.config()));
			case "file":
				return SimpleCommandArrayOutput.fromFile(new File(cfg.config.getAsString()));
			case "search":
				return ((SimpleCommandArrayOutput) simpleCommandFromJson(gson, cfg.config()))::search;
			default:
				throw new IllegalArgumentException("unkown simple command type: " + cfg.type());
		}
	}
	
	@Data
	private static class SimpleCommandConfig {
		
		private String type;
		private JsonElement config;
	}
}
