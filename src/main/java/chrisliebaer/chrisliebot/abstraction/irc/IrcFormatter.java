package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.MessageBuilder.Formatting;
import org.kitteh.irc.client.library.util.Format;

@UtilityClass
public class IrcFormatter {
	
	public static String format(Object format, String s) {
		
		// handle chrisliebot formattings
		if (format instanceof ChrislieFormat)
			return format((ChrislieFormat) format, s);
		
		// handle native irc formats
		if (format instanceof Format)
			return format((Format) format, s);
		
		// handle discord formats and convert to irc
		if (format instanceof Formatting)
			return format((Formatting) format, s);
		
		// just assume it's some format code
		if (format instanceof String)
			return format + s + Format.RESET;
		
		throw new UnsupportedOperationException("unkown format: " + format);
	}
	
	public static String format(ChrislieFormat format, String s) {
		switch (format) {
			
			case HIGHLIGHT:
				return format(Format.TEAL, s);
			case BOLD:
				return format(Format.BOLD, s);
			case ITALIC:
				return format(Format.ITALIC, s);
			case UNDERLINE:
				return format(Format.UNDERLINE, s);
			case CODE:
				return "`" + s + "`";
			default:
				throw new UnsupportedOperationException("unkown format: " + format);
		}
	}
	
	public static String format(Format format, String s) {
		return format.toString() + s + Format.RESET;
	}
	
	public static String format(Formatting format, String s) {
		switch (format) {
			case ITALICS:
				return format(Format.ITALIC, s);
			case BOLD:
				return format(Format.BOLD, s);
			case UNDERLINE:
				return format(Format.UNDERLINE, s);
			case STRIKETHROUGH:
				return "~~" + s + "~~";
			case BLOCK:
				return "`" + s + "`";
			default:
				throw new UnsupportedOperationException("unkown format: " + format);
		}
	}
}
