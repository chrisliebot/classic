package chrisliebaer.chrisliebot.protocol.irc;

import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.command.CapabilityRequestCommand;
import org.kitteh.irc.client.library.element.CapabilityState;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesAcknowledgedEvent;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesRejectedEvent;
import org.kitteh.irc.client.library.event.capabilities.CapabilitiesSupportedListEvent;
import org.kitteh.irc.client.library.feature.CapabilityManager;

import java.util.Optional;

/**
 * Adding this handler to the event processor will enable the "echo-message" capability if supported by the server.
 */
@Slf4j
public class EchoCapHandler {
	
	@Handler
	public void capList(CapabilitiesSupportedListEvent ev) {
		if (ev.isNegotiating()) {
			Optional<CapabilityState> opt = ev.getSupportedCapabilities()
					.stream().filter(s -> s.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE)).findFirst();
			
			if (opt.isEmpty()) {
				log.warn("echo-message is not supported by server, will not be able to log own messages");
				return;
			}
			
			// request echo cap
			new CapabilityRequestCommand(ev.getClient()).enable(CapabilityManager.Defaults.ECHO_MESSAGE).execute();
		}
	}
	
	@Handler
	public void capAck(CapabilitiesAcknowledgedEvent ev) {
		if (ev.getAcknowledgedCapabilities().stream().anyMatch(c -> c.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE))) {
			log.info("enabled echo-message capability");
		}
	}
	
	@Handler
	public void capAck(CapabilitiesRejectedEvent ev) {
		if (ev.getRejectedCapabilitiesRequest().stream().anyMatch(c -> c.getName().equalsIgnoreCase(CapabilityManager.Defaults.ECHO_MESSAGE))) {
			log.error("failed to enable echo-message, will not be able to react to own messages (including logging)");
		}
	}
}
