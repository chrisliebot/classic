package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.Message;

@FunctionalInterface
public interface ChatTriggered {
	
	public void onMessage(Message m);
}
