package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;

public class NSFWSelector implements Selector {
	
	@Override
	public boolean check(ChrislieMessage message) {
		return message.channel().isNSFW();
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return channel.isNSFW();
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
}
