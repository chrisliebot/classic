package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

/**
 * This selector matches if the given user is inside the current channel. In other words: The selector only matches if
 * the given user can observe the message.
 */
public class UserExistsInChannel implements Selector {
	
	private String user;
	
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		user = json.getAsString();
		if (user == null)
			throw new SelectorException("user must be set");
	}
	
	@Override
	public boolean check(ChrislieMessage message) {
		var user = message.user();
		var channel = message.channel();
		return check(channel) || check(user);
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return user.identifier().equals(this.user);
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return channel.user(user).isPresent();
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieGuild guild) {
		return false;
	}
}
