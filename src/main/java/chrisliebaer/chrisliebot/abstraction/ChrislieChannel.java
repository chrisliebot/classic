package chrisliebaer.chrisliebot.abstraction;

import java.util.List;
import java.util.Optional;

/**
 * This interface represents a message channel that is capable of exchanging messages.
 */
public interface ChrislieChannel extends ServiceAttached {
	
	/**
	 * @return An internal identifier of this channel. Should be used when storing channel associated data.
	 */
	public String identifier();
	
	/**
	 * @return The display name which should be used to refer to this channel in human facing messages.
	 */
	public String displayName();
	
	/**
	 * @return {@code true} if this channel represents a one to one communication.
	 */
	public boolean isDirectMessage();
	
	/**
	 * @return A list of all users that are currently in this channel.
	 */
	public List<? extends ChrislieUser> users();
	
	/**
	 * @return {@code true} if this channel is marked as a NSFW channel.
	 */
	public default boolean isNSFW() {
		return false;
	}
	
	/**
	 * @return {@code true} if the bot can write messages to this channel.
	 */
	public default boolean canTalk() {
		return true;
	}
	
	/**
	 * @param identifier The identifier of the user.
	 * @return A potential user if the user can be found in this channel.
	 */
	public default Optional<? extends ChrislieUser> user(String identifier) {
		return users().stream()
				.filter(user -> user.identifier().equals(identifier))
				.findAny();
	}
	
	/**
	 * This method attempts to resolve the given call name to a user. This method is expected to be used in commands
	 * where users might refer to other users.
	 *
	 * @param callName A string that users of this service would normaly use to refer to another user.
	 * @return A potential user that this call name might refer to.
	 */
	public Optional<? extends ChrislieUser> resolve(String callName);
	
	/**
	 * Some services allow channels to be grouped in guilds. A guild is a collection of channels that usually resemble a
	 * somewhat connected community. You must not make assumptions on the existance of a guild because of the value
	 * returned by {@link #isDirectMessage()}.
	 *
	 * @return An optional guild, if this channel is part of a guild.
	 */
	public Optional<? extends ChrislieGuild> guild();
	
	/**
	 * Creates a new output instance for this channel.
	 *
	 * @param limiterConfig The LimiterConfig that will be used to limit the posted message by the returned
	 *                      ChrislieOutput.
	 * @return A ChrislieOutput instance that can be used to post to this channel.
	 */
	public ChrislieOutput output(LimiterConfig limiterConfig);
}
