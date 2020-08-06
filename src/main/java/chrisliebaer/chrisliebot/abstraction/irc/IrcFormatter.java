package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.MessageBuilder.Formatting;
import org.kitteh.irc.client.library.util.Format;

@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
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
		return switch (format) {
			case HIGHLIGHT -> format(Format.TEAL, s);
			case BOLD -> format(Format.BOLD, s);
			case ITALIC -> format(Format.ITALIC, s);
			case UNDERLINE -> format(Format.UNDERLINE, s);
			case CODE -> "`" + s + "`";
		};
	}
	
	public static String format(Format format, String s) {
		return format.toString() + s + Format.RESET;
	}
	
	public static String format(Formatting format, String s) {
		return switch (format) {
			case ITALICS -> format(Format.ITALIC, s);
			case BOLD -> format(Format.BOLD, s);
			case UNDERLINE -> format(Format.UNDERLINE, s);
			case STRIKETHROUGH -> "~~" + s + "~~";
			case BLOCK -> "`" + s + "`";
		};
	}
}
