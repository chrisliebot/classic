package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;

public class AcceptAllSelector implements Selector {
	
	@Override
	public boolean check(ChrislieMessage message) {
		return true;
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return true;
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return true;
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return true;
	}
}
