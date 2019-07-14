package chrisliebaer.chrisliebot.abstraction;

public interface ChrislieMessage extends ServiceAttached {
	
	public ChrislieChannel channel();
	
	public ChrislieUser user();
	
	public String message();
}
