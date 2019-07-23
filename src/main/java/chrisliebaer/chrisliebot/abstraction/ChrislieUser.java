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
	public Optional<String> identifier();
	
	/**
	 * Some services don't require users to have a fixed identifier. While this may cause security issues for certain features, other functions might work fine
	 * with a best effordt identifier. This identifier tries it's best to provied a constant identifier but can fail.
	 *
	 * @return A best effort attempt at providing a indentifier.
	 */
	public default String softIdentifer() {
		return identifier().orElseThrow();
	}
	
	/**
	 * @return The string that should be used to {@code ping} the user.
	 */
	public String mention();
	
	/**
	 * @return {@code true} if the user is an admin on this bot instance.
	 */
	public boolean isAdmin();
	
	/**
	 * @return An instance of a ChrislieChannel that can be used to directly contact the user.
	 */
	public ChrislieChannel directMessage();
}
