package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.C;
import org.kitteh.irc.client.library.element.MessageReceiver;

import java.util.Arrays;

public class LimiterConfig {
	
	private int maxLines = 10;
	private int maxLenght = 1000;
	private boolean stripLineBreak = false;
	private boolean stripOverflow = false;
	
	public static LimiterConfig create() {
		return new LimiterConfig();
	}
	
	public LimiterConfig maxLines(int max) {
		maxLines = max;
		return this;
	}
	
	public LimiterConfig maxLength(int max) {
		maxLenght = max;
		return this;
	}
	
	public LimiterConfig stripLineBreak() {
		stripLineBreak = true;
		return this;
	}
	
	public LimiterConfig stripOverflowLines() {
		stripOverflow = true;
		return this;
	}
	
	public LimiterConfig send(MessageReceiver receiver, String message) {
		
		// remove illegal characters
		message = message.replace("\0", "");
		
		if (stripLineBreak)
			message = C.NEWLINE_PATTERN.matcher(message).replaceAll("");
		
		/* due to the nature of the split method, providing a limit will automatically merge all overflow lines
		 * so if we wan't to strip overflow lines, we simply provide no limit and use the stream limit to remove excess lines
		 */
		Arrays.stream(C.NEWLINE_PATTERN.split(message, stripOverflow ? 0 : maxLines + 1))
				.map(s -> s.length() > maxLenght ? s.subSequence(0, maxLenght) + " [...]" : s)
				.limit(maxLines)
				.forEachOrdered(receiver::sendMultiLineMessage);
		
		return this;
	}
}
