package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

public class ServiceIdentifierSelector implements Selector {
	
	private String identifier;
	
	@Override
	public boolean check(ChrislieMessage message) {
		return check(message.service());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return check(user.service());
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return check(channel.service());
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return service.identifier().equals(identifier);
	}
	
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		identifier = json.getAsString();
	}
}
