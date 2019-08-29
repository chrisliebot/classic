package chrisliebaer.chrisliebot.abstraction;

import java.util.Optional;

/**
 * This interface represents an individual user of an abstract chat service.
 */
public interface ChrislieUser extends ServiceAttached {
	
	/**
	 * @return The display name which should be used to talk about the user in human facing messages.
	 */
	public String displayName();
	
	/**
	 * @return An internal identifier of a user. Should be used when storing user associated data.
	 */
	public String identifier();
	
	/**
	 * @return The string that should be used to {@code ping} the user.
	 */
	public String mention();
	
	/**
	 * @return An instance of a ChrislieChannel that can be used to directly contact the user. Can fail it the user has blocked us or we are otherwise unable to contact
	 * user.
	 */
	public Optional<? extends ChrislieChannel> directMessage();
}
