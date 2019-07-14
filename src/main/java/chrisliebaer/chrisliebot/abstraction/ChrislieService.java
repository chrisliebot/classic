package chrisliebaer.chrisliebot.abstraction;

import java.util.Optional;

public interface ChrislieService {
	
	public Optional<? extends ChrislieChannel> channel(String identifier);
	
	public Optional<? extends ChrislieUser> user(String identifier);
}
