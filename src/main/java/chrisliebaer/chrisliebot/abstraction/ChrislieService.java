package chrisliebaer.chrisliebot.abstraction;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A service abstracts a chat protocol. Very few methods must actually be implemented by this interface to function properly.
 */
public interface ChrislieService {
	
	/**
	 * @return
	 */
	public String identifier();
	
	/**
	 * Implementations of this method must block until the service is fully operational. This does not mean that the service has to be connected to the network unless a
	 * connection is required in order to be fully configured. It does mean however that the service is fully ready to expect calls via Chrisliebot's abstraction layer.
	 *
	 * @throws Exception If a unrecoverable error is occured that prevents the service from ever entering a fully operational state.
	 */
	public void awaitReady() throws Exception;
	
	/**
	 * @param sink
	 * @return
	 */
	public ChrislieService sink(@Nullable Consumer<ChrislieMessage> sink);
	
	/**
	 * @param identifier
	 * @return
	 */
	public Optional<? extends ChrislieChannel> channel(String identifier);
	
	/**
	 * @param identifier
	 * @return
	 */
	public Optional<? extends ChrislieUser> user(String identifier);
	
	/**
	 * @param identifier
	 * @return
	 */
	public Optional<? extends ChrislieGuild> guild(String identifier);
	
	/**
	 * Implementing service is expected to, if possible, drop connection and reconnect to network.
	 */
	public default void reconnect() {}
	
	/**
	 * Called by Chrisliebot to shutdown this service. The implementation should block until it has completly shut down.
	 *
	 * @throws Exception If a proper shutdown is not possible.
	 */
	public void exit() throws Exception;
}
