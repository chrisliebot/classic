package chrisliebaer.chrisliebot.command.simplecommand;

import chrisliebaer.chrisliebot.C;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public final class SimpleCommandMappedOutput implements SimpleCommandOutput {
	
	
	private static final String START_TOKEN = "${";
	private static final String END_TOKEN = "}";
	private static final String ESCAPE_TOKEN = "\\";
	
	private static final String INPUT_MAPPING = "INPUT";
	private static final String DEFAULT_MAPPING = "DEFAULT";
	private static Map<String, SimpleCommandOutput> map;
	
	private Map<String, SimpleCommandOutput> mapping = new HashMap<>();
	
	private SimpleCommandMappedOutput(@NonNull Map<String, SimpleCommandOutput> mapping) {
		if (mapping != null)
			mapping.forEach(this::addMapping); // required to enforce prevention of duplicates and special INPUT
		
		// check that a default generator has been specified
		Preconditions.checkState(this.mapping.containsKey(DEFAULT_MAPPING), "no mapping with name DEFAULT given");
	}
	
	public void addMapping(@NonNull String name, @NonNull SimpleCommandOutput output) {
		Preconditions.checkArgument(!mapping.containsKey(name), "Mapping already exists");
		Preconditions.checkArgument(!INPUT_MAPPING.equals(name), "Mapping " + INPUT_MAPPING + " is reserved keyword");
		mapping.put(name, output);
	}
	
	@Override
	public String out(String in) {
		/* Substitution uses multiple generators at once. While many generators can be added, a few names are handled special
		 * The DEFAULT generator is used to generate the initial substitution pattern
		 * The virtual INPUT generator will always return the given input.
		 */
		
		StringSubstitutor sub = new StringSubstitutor(key -> {
			if (INPUT_MAPPING.equals(key))
				return in;
			else
				return getMapping(key).out(in);
		});
		
		return sub.replace(getMapping(DEFAULT_MAPPING).out(null));
	}
	
	private SimpleCommandOutput getMapping(String key) {
		return mapping.getOrDefault(key, SimpleCommandOutput.staticCommandOutput("$${" + key + "}"));
	}
	
	public static SimpleCommandMappedOutput fromMap(@NonNull Map<String, SimpleCommandOutput> map) {
		return new SimpleCommandMappedOutput(map);
	}
	
	// prevents famous Keksbot exploit if userinput is "${INPUT}"
	public static SimpleCommandOutput escape(@NonNull SimpleCommandOutput out) {
		return in -> {
			// escape input
			String escaped = C.escapeStrSubstitution(in);
			
			return out.out(escaped);
		};
	}
}
