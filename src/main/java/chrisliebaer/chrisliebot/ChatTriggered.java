package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;

@FunctionalInterface
public interface ChatTriggered {
	
	public void onMessage(ChrislieMessage m);
}
