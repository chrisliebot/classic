package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import net.dv8tion.jda.api.entities.MessageChannel;

public interface DiscordChannel extends ChrislieChannel {
	
	public abstract MessageChannel messageChannel();
}
