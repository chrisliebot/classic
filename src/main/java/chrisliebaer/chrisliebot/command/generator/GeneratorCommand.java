package chrisliebaer.chrisliebot.command.generator;

import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.SerializedOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static chrisliebaer.chrisliebot.C.escapeStrSubstitution;

public class GeneratorCommand implements ChrislieListener.Command {
	
	private SerializedOutput output;
	private Map<String, Generator> generators;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		var cfg = gson.fromJson(json, Config.class);
		output = cfg.output;
		
		generators = new HashMap<>(cfg.generators.size());
		for (var e : cfg.generators.entrySet())
			generators.put(e.getKey(), loadGenerator(gson, e.getValue()));
	}
	
	private Generator loadGenerator(GsonValidator gson, GeneratorConfig cfg) throws ListenerException {
		switch (cfg.type) {
			case "static":
				return new StaticGenerator(cfg.cfg.getAsString());
			case "file":
				return new FileGenerator(gson.fromJson(cfg.cfg, FileGenerator.Config.class));
			default:
				throw new ListenerException("unkown generator type: " + cfg.type);
		}
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var reply = invc.reply();
		var substitutor = new CachingSubstitutor(invc);
		output.apply(reply, substitutor::substitute);
		
		// some generators will depend on the input string (regex search) and thereforce not always be able to provide an output
		if (!substitutor.generatorEmpty)
			reply.send();
	}
	
	private class CachingSubstitutor extends StrLookup {
		
		// indicates that at least one generator failed to provide an output
		private boolean generatorEmpty;
		
		// TODO: rewrite this shit with our own substitutor since the apache one does not allow for proper exception handling and the cache also sucks donkey dicks, also check if we can ditch the apache string lib
		@SuppressWarnings("ThisEscapedInObjectConstruction")
		private final StrSubstitutor substitutor = new StrSubstitutor(this);
		private final LoadingCache<String, Map<String, String>> cache;
		
		public CachingSubstitutor(Invocation invocation) {
			// instances a loading cache that will use the parents generators with the given invocation to create inputs
			cache = CacheBuilder.newBuilder()
					.build(new CacheLoader<>() {
						@Override
						public Map<String, String> load(String key) throws ListenerException {
							var generator = generators.get(key);
							
							if (generator == null)
								return null;
							
							return generator.generate(invocation, GeneratorCommand.this);
						}
					});
			
			// insert message generator that allows access to user sender data
			var msg = invocation.msg();
			var user = msg.user();
			HashMap<String, String> map = new HashMap<>();
			map.put("arg", escapeStrSubstitution(invocation.arg()));
			map.put("displayName", escapeStrSubstitution(user.displayName()));
			map.put("mention", escapeStrSubstitution(user.mention()));
			map.put("guild.displayName", escapeStrSubstitution(msg.channel().guild().map(ChrislieGuild::displayName).orElse(msg.channel().displayName())));
			
			cache.put("message", map);
		}
		
		public String substitute(String s) {
			return substitutor.replace(s);
		}
		
		@Override
		public String lookup(String key) {
			// keys should be in format generator.field, if not, we assume generator.DEFAULT
			var args = key.split("\\.", 2);
			var gen = args[0];
			var field = Generator.DEFAULT;
			if (args.length > 1)
				field = args[1];
			
			try {
				Map<String, String> outMap = cache.get(gen);
				if (outMap == null)
					return "UNKOWN_LOOKUP(" + key + ")";
				
				return outMap.get(field);
			} catch (ExecutionException e) {
				return null;
			}
		}
	}
	
	private static class Config {
		
		private @NotNull SerializedOutput output;
		private Map<String, GeneratorConfig> generators = Map.of();
	}
	
	private static class GeneratorConfig {
		
		private String type;
		private JsonElement cfg;
	}
}
