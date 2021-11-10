package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.Chrisliebot;

public interface ServiceAttached {
	
	public ChrislieService service();
	
	public default Chrisliebot bot() {
		return service().bot();
	}
}
