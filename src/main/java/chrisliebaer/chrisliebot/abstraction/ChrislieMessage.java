package chrisliebaer.chrisliebot.abstraction;

public interface ChrislieMessage extends ServiceAttached {
	
	public ChrislieChannel channel();
	
	public ChrislieUser user();
	
	public String message();
	
	/**
	 * Helper method for providing a quick response without having to deal with the abstraction layer.
	 *
	 * @param s The reponse text.
	 */
	public default void reply(String s) {
		channel().output().plain(s).send();
	}
	
	
	public default ChrislieOutput reply() {
		return channel().output();
	}
}
