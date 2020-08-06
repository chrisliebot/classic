package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.irc.IrcFormatter;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.MessageBuilder.Formatting;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.kitteh.irc.client.library.util.Format;

@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
@UtilityClass
public class DiscordFormatter {
	
	public static String format(Object format, String s) {
		
		// convert irc->chrisliebot
		if (format instanceof Format)
			format = IrcFormatter.irc2ChrislieFormat((Format) format);
		
		// convert discord->chrisliebot (since jda requires message builder for applying formatting)
		if (format instanceof Formatting)
			format = discord2ChrislieFormat((Formatting) format);
		
		// handle chrisliebot formattings
		if (format instanceof ChrislieFormat)
			return format((ChrislieFormat) format, s);
		
		// just assume it's some special code we don't know and enclose string
		if (format instanceof String)
			return format + s + format;
		
		throw new UnsupportedOperationException("unkown format: " + format);
	}
	
	public static String format(ChrislieFormat format, String s) {
		// there is no way to format string in jda without using message builder, so we do it manually
		return switch (format) {
			case HIGHLIGHT, BOLD -> MarkdownUtil.bold(s);
			case ITALIC -> MarkdownUtil.italics(s);
			case UNDERLINE -> MarkdownUtil.underline(s);
			case CODE -> MarkdownUtil.monospace(s);
			case BLOCK -> MarkdownUtil.codeblock(s);
			case SPOILER -> MarkdownUtil.spoiler(s);
			case QUOTE -> MarkdownUtil.quote(s);
			case STRIKETHROUGH -> MarkdownUtil.strike(s);
			case NONE -> s;
		};
	}
	
	public static ChrislieFormat discord2ChrislieFormat(Formatting format) {
		return switch (format) {
			case ITALICS -> ChrislieFormat.ITALIC;
			case BOLD -> ChrislieFormat.BOLD;
			case UNDERLINE -> ChrislieFormat.UNDERLINE;
			case STRIKETHROUGH -> ChrislieFormat.STRIKETHROUGH;
			case BLOCK -> ChrislieFormat.BLOCK;
		};
	}
}
