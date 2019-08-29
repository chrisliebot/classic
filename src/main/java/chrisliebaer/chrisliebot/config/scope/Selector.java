package chrisliebaer.chrisliebot.config.scope;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;


public interface Selector {
	
	public boolean check(ChrislieMessage message);
	
	public boolean check(ChrislieUser user);
	
	public boolean check(ChrislieChannel channel);
	
	public boolean check(ChrislieService service);
	
	public default void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {}
	
	/**
	 * Thrown to indicate that the selector was unable to load the provided config.
	 */
	public static class SelectorException extends Exception {
		
		public SelectorException(String message) {
			super(message);
		}
		
		public SelectorException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public SelectorException(Throwable cause) {
			super(cause);
		}
	}
}
