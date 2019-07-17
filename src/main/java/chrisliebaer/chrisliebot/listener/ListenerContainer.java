package chrisliebaer.chrisliebot.listener;

import chrisliebaer.chrisliebot.ChatTriggered;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.CommandContainer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Bundles a {@link ChatListener} with it's triggers.
 */
@Slf4j
@AllArgsConstructor
public class ListenerContainer implements ChatTriggered {
	
	private ChatListener listener;
	private List<CommandContainer> trigger;
	
	@Override
	public void onMessage(ChrislieMessage m) {
		if (listener.test(m)) {
			for (CommandContainer container : trigger) {
				if (container.requireAdmin() && !m.user().isAdmin())
					continue;
				
				container.execute(m, null);
			}
		}
	}
}
