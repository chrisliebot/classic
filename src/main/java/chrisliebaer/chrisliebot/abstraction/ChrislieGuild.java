package chrisliebaer.chrisliebot.abstraction;

import java.util.Collection;
import java.util.stream.Collectors;

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
	 * @return A list of all users that are currently part of this guild.
	 */
	public default Collection<? extends ChrislieUser> users() {
		return channels().stream()
				.map(ChrislieChannel::users)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}
	
	/**
	 * @return A list of all channels that are part of this guild.
	 */
	public Collection<? extends ChrislieChannel> channels();
	
	// TODO: get user, call name? (check chrisliechannel) do we want to introduce a new type for guildusers? (discord offers that)
}
