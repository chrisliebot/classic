package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.abstraction.irc.IrcChannel;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.kitteh.irc.client.library.element.Channel;

import java.util.List;

public class IrcChannelFlagSelector implements Selector {
	
	private List<Character> chars;
	
	@SuppressWarnings("EmptyClass")
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		chars = gson.fromJson(json, new TypeToken<List<Character>>() {}.getType());
		if (chars == null)
			throw new SelectorException("char list must not be null");
	}
	
	@Override
	public boolean check(ChrislieMessage message) {
		return check(message.channel());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		if (!(channel instanceof IrcChannel))
			return false;
		
		var chan = (IrcChannel) channel;
		Channel c = chan.channel();
		
		for (char ch : chars) {
			if (c.getModes().containsMode(ch))
				return true;
		}
		return false;
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
