package chrisliebaer.chrisliebot.command.simplecommand;

import org.kitteh.irc.client.library.util.CtcpUtil;

/**
 * Part of the modular simple command system. Implementations are expected to produce a single string from the given input.
 */
@FunctionalInterface
public interface SimpleCommandOutput {
	
	public String out(String in);
	
	public static SimpleCommandOutput staticCommandOutput(String out) {
		return in -> out;
	}
	
	public static SimpleCommandOutput ctcpActionWrapper(SimpleCommandOutput out) {
		return in -> CtcpUtil.toCtcp("ACTION " + out.out(in));
	}
}
