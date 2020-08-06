package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.command.ChrislieListener;

/**
 * This listener can be used to override existing listeners, since it doesn't provide any logic and will just silently
 * eat the invocation.
 */
public class NullCommand implements ChrislieListener.Command {
	
	@Override
	public void execute(Invocation invc) throws ListenerException {}
}
