package chrisliebaer.chrisliebot.abstraction;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public interface ChrislieService {
	
	public void sink(@Nullable Consumer<ChrislieMessage> sink);
	
	public Optional<? extends ChrislieChannel> channel(String identifier);
	
	public Optional<? extends ChrislieUser> user(String identifier);
	
	public void reconnect();
	
	public void exit() throws Exception;
}
