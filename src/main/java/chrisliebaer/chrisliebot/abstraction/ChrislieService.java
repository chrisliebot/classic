package chrisliebaer.chrisliebot.abstraction;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A service abstracts a chat protocol. Very few methods must actually be implemented by this interface to function properly.
 */
public interface ChrislieService {
	
	/**
	 * @return Unique indentifier of this service.
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
	 * @param sink The new sink to pump messages to.
	 * @return This service for method chaining.
	 */
	public ChrislieService sink(@Nullable Consumer<ChrislieMessage> sink);
	
	/**
	 * @param identifier The unique channel identifier of a channel that's owned by this service.
	 * @return An optional containing the channel if the given identifier could be resolved.
	 */
	public Optional<? extends ChrislieChannel> channel(String identifier);
	
	/**
	 * @param identifier The unique user identifier of a user that's owned by this service.
	 * @return An optional containing the user if the given identifier could be resolved.
	 */
	public Optional<? extends ChrislieUser> user(String identifier);
	
	/**
	 * @param identifier The unique user identifier of a guild that's owned by this service.
	 * @return An optional containing the guild if the given identifier could be resolved.
	 */
	public Optional<? extends ChrislieGuild> guild(String identifier);
	
	/**
	 * Implementing service is expected to, if possible, drop connection and reconnect to network.
	 */
	public default void reconnect() {}
	
	/**
	 * Called by Chrisliebot to shutdown this service. The implementation should block until it has completly shut down. After returning from this method, the service
	 * must no longer interact with the Chrisliebot framework.
	 *
	 * @throws ServiceException If a proper shutdown is not possible.
	 */
	public void exit() throws ServiceException;
	
	public static class ServiceException extends Exception {
		
		public ServiceException() {
		}
		
		public ServiceException(String message) {
			super(message);
		}
		
		public ServiceException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ServiceException(Throwable cause) {
			super(cause);
		}
	}
}
