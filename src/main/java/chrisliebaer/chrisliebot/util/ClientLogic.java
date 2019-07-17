package chrisliebaer.chrisliebot.util;

import chrisliebaer.chrisliebot.C;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Invoke;
import org.kitteh.irc.client.library.event.channel.ChannelInviteEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveCommandEvent;
import org.kitteh.irc.client.library.feature.filter.CommandFilter;

/**
 * Takes care of higher level client logic that would normaly be done by the user themself.
 */
@Slf4j
public class ClientLogic {
	
	//TODO: keep track of incoming lines and reconnect if timeout

	
	// znc will keep connection lingering after sending an error
	@CommandFilter("ERROR")
	@Handler(delivery = Invoke.Synchronously)
	public void onDisconnect(ClientReceiveCommandEvent ev) {
		log.error("server terminated connection, reason: {}", ev.getParameters());
		ev.getClient().reconnect("Received ERROR from server");
	}
	
	// library fails to respond to ping events, instead sends ping every minute, not enought
	@CommandFilter("PING")
	@Handler(delivery = Invoke.Synchronously)
	public void onPing(ClientReceiveCommandEvent ev) {
		var param = ev.getParameters();
		String r = "Chrisliebot";
		if (param.size() >= 1 && param.get(0) != null && !param.get(0).isEmpty())
			r = param.get(0);
		ev.getClient().sendRawLine("PONG :" + r);
	}
	
	@Handler
	public void onInvite(ChannelInviteEvent ev) {
		if (ev.getTarget().equals(ev.getClient().getNick())) {
			log.info(C.LOG_PUBLIC, "received invite from {} to join {}", ev.getActor().getName(), ev.getChannel().getName());
			ev.getChannel().join();
		}
	}
}
