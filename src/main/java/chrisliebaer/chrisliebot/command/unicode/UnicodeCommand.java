package chrisliebaer.chrisliebot.command.unicode;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UnicodeCommand implements ChrisieCommand {
	
	private static final ErrorOutputBuilder ERROR_NUMBER_PARSE = ErrorOutputBuilder.generic("Das sah zwar gut aus, aber ich habs trotzdem nicht gerafft.");
	private static final ErrorOutputBuilder ERROR_INDAVLID_CP = ErrorOutputBuilder.generic("Ungültiger Codepoint, versuch es mal mit einem anderen.");
	
	private static final int MAX_CODEPOINT_DISPLAY = 5;
	
	private static final Pattern CODEPOINT_INPUT = Pattern.compile("^U\\+(?<cp>[0-9A-Fa-f]{1,8})$");
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		arg = arg.trim();
		
		List<Integer> cps;
		var matcher = CODEPOINT_INPUT.matcher(arg);
		if (matcher.matches()) { // matches on input that is describing raw code points without encoding
			try {
				cps = new ArrayList<>(1);
				cps.add((int) Long.parseLong(matcher.group("cp"), 16));
			} catch (NumberFormatException e) {
				ERROR_NUMBER_PARSE.write(m);
				return;
			}
		} else { // parse input as is
			cps = arg.codePoints().boxed().collect(Collectors.toList());
		}
		
		StringJoiner joiner = new StringJoiner(", ");
		try {
			for (Integer cp : cps) {
				String s = String.valueOf(Character.toChars(cp));
				joiner.add(s);
			}
		} catch (IllegalArgumentException e) {
			ERROR_INDAVLID_CP.write(m);
			return;
		}
		String input = joiner.toString();
		
		var reply = m.reply();
		reply.title("Codepointanalyse (limitiert auf " + MAX_CODEPOINT_DISPLAY + ")");
		reply.field("Eingabe", input);
		reply.field("Anzahl Codepoints", String.valueOf(cps.size()));
		
		cps.stream().limit(MAX_CODEPOINT_DISPLAY).forEachOrdered(i -> printCodePoint(i, reply.description()));
		
		var convert = reply.convert();
		convert
				.appendEscape("Eingabe war: [")
				.appendEscape(input, ChrislieFormat.HIGHLIGHT)
				.appendEscape("] Anzahl Codepoints: ")
				.appendEscape(String.valueOf(cps.size()), ChrislieFormat.HIGHLIGHT)
				.appendEscape(" (Ausgabe limitiert auf maximal " + MAX_CODEPOINT_DISPLAY + " Codepoints)")
				.newLine()
				.appendSub("${description}");
		
		reply.send();
	}
	
	private void printCodePoint(int cp, PlainOutput out) {
		Optional<String> name = Optional.ofNullable(Character.getName(cp));
		var cpStr = Integer.toHexString(cp).toUpperCase();
		var str = Character.toString(cp);
		var utf8 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_8)).toUpperCase();
		var utf16 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_16LE)).toUpperCase();
		
		out
				.appendEscape("Display: ").appendEscape(str, ChrislieFormat.HIGHLIGHT).appendEscape(", ")
				.appendEscape("Codepoint: U+").appendEscape(cpStr, ChrislieFormat.HIGHLIGHT).appendEscape(", ")
				.appendEscape("UTF-8: 0x").appendEscape(utf8, ChrislieFormat.HIGHLIGHT).appendEscape(", ")
				.appendEscape("UTF-16LE: 0x").appendEscape(utf16, ChrislieFormat.HIGHLIGHT).appendEscape(", ")
				.appendEscape("Name: ").appendEscape(name.orElse("UNBEKANNT"), ChrislieFormat.HIGHLIGHT)
				.newLine();
	}
}
