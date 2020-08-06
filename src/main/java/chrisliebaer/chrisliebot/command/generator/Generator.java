package chrisliebaer.chrisliebot.command.generator;

import chrisliebaer.chrisliebot.command.ChrislieListener;

import java.util.Map;

/**
 * A generator is an abstract description of an entity that is providing (random) output for commands. A generator may
 * return a set of linked strings but must, at the very least, contain a default output.
 */
public interface Generator {
	
	public static final String DEFAULT = "DEFAULT";
	
	public Map<String, String> generate(ChrislieListener.Invocation invc, GeneratorCommand command) throws ChrislieListener.ListenerException;
}
