package chrisliebaer.chrisliebot.command.wrapper;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

/**
 * Remebers past executions and can be configured with a timeout or a chance to prevent execution of wrapped command.
 */
public class BrainTrigger implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
	
	}
}
