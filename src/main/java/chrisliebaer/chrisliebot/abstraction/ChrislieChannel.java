package chrisliebaer.chrisliebot.abstraction;

import java.util.List;
import java.util.Optional;

/**
 * This interface represents a message channel that is capable of exchanging messages.
 */
public interface ChrislieChannel extends ServiceAttached {
	
	/**
	 * @return The display name which should be used to refer to this channel in human facing messages.
	 */
	public String displayName();
	
	/**
	 * @return An internal identifier of this channel. Should be used when storing channel associated data.
	 */
	public String identifier();
	
	/**
	 * @return {@code true} if this channel represents a one to one communication.
	 */
	public boolean isDirectMessage();
	
	/**
	 * @return A list of all users that are currently in this channel.
	 */
	public List<? extends ChrislieUser> users();
	
	/**
	 * @param identifier The identifier of the user.
	 * @return A potential user if the user can be found in this channel.
	 */
	public Optional<? extends ChrislieUser> user(String identifier);
	
	/**
	 * This method attempts to resolve the given call name to a user. This method is expected to be used in commands where users might refer to other users.
	 *
	 * @param callName A string that users of this service would normaly use to refer to another user.
	 * @return A potential user that this call name might refer to.
	 */
	public Optional<? extends ChrislieUser> resolve(String callName);
	
	/**
	 * Creates a new Output instance that is associated with this channel.
	 *
	 * @param limiterConfig
	 * @return A ChrislieOutput instance that can be used to post to this channel.
	 */
	public ChrislieOutput output(LimiterConfig limiterConfig);
	
	public default ChrislieOutput output() {
		return output(LimiterConfig.create());
	}
}
