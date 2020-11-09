package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

public class ChannelSelector implements Selector {
	
	private String channel;
	
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		channel = json.getAsString();
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
		return channel.identifier().equalsIgnoreCase(this.channel);
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
}
