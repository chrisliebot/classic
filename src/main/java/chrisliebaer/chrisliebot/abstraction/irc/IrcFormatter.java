package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordFormatter;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.MessageBuilder.Formatting;
import org.kitteh.irc.client.library.util.Format;

@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
@UtilityClass
public class IrcFormatter {
	
	public static String format(Object format, String s) {
		
		// convert discord->chrisliebot
		if (format instanceof Formatting)
			format = DiscordFormatter.discord2ChrislieFormat((Formatting) format);
		
		// handle irc directly
		if (format instanceof Format)
			return format((Format) format, s);
		
		// handle chrisliebot formattings
		if (format instanceof ChrislieFormat)
			return format((ChrislieFormat) format, s);
		
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
			case QUOTE -> "\"" + s + "\"";
			case STRIKETHROUGH -> "~~" + s + "~~";
			case NONE, BLOCK, SPOILER -> s;
		};
	}
	
	public static String format(Format format, String s) {
		return format.toString() + s + Format.RESET;
	}
	
	public static ChrislieFormat irc2ChrislieFormat(Format format) {
		return switch (format) {
			case BOLD -> ChrislieFormat.BOLD;
			case ITALIC -> ChrislieFormat.ITALIC;
			case UNDERLINE -> ChrislieFormat.UNDERLINE;
			default -> ChrislieFormat.NONE;
		};
	}
}
