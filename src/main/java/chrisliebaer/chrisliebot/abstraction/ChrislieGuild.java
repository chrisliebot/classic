package chrisliebaer.chrisliebot.abstraction;

import java.util.Collection;

public interface ChrislieGuild extends ServiceAttached {
	
	/**
	 * @return An internal identifier of this guild. Should be used when storing guild associated data.
	 */
	public String identifier();
	
	/**
	 * @return The display name which should be used to refer to this guild in human facing messages.
	 */
	public String displayName();
	
	/**
	 * @param user The user to look for.
	 * @return {@code true} if the given user is currently part of this guild.
	 */
	public boolean isMember(ChrislieUser user);
	
	/**
	 * @return A list of all channels that are part of this guild.
	 */
	public Collection<? extends ChrislieChannel> channels();
	
	// TODO: get user, call name? (check chrisliechannel) do we want to introduce a new type for guildusers? (discord offers that)
}
