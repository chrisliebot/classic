package chrisliebaer.chrisliebot.command.unicode;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UnicodeCommand implements CommandExecutor {
	
	private static final int MAX_CODEPOINT_DISPLAY = 5;
	
	private static final Pattern CODEPOINT_INPUT = Pattern.compile("^U\\+(?<cp>[0-9A-Fa-f]{1,8})$");
	
	@Override
	public void execute(Message m, String arg) {
		arg = arg.trim();
		
		List<Integer> cps;
		var matcher = CODEPOINT_INPUT.matcher(arg);
		if (matcher.matches()) { // matches on input that is describing raw code points without encoding
			try {
				cps = new ArrayList<>(1);
				cps.add((int) Long.parseLong(matcher.group("cp"), 16));
			} catch (NumberFormatException e) {
				m.reply(C.error("Das sah zwar gut aus, aber ich habs trotzdem nicht gerafft."));
				return;
			}
		} else { // parse input as is
			cps = arg.codePoints().boxed().collect(Collectors.toList());
		}
		
		String input = cps.stream().map(cp -> String.valueOf(Character.toChars(cp))).collect(Collectors.joining(", "));
		m.reply("Eingabe war: " + C.highlight(input) + ", zeige ersten " + MAX_CODEPOINT_DISPLAY + " von " + cps.size() + " Codepoints.");
		cps.stream().limit(MAX_CODEPOINT_DISPLAY).forEachOrdered(i -> printCodePoint(i, m));
	}
	
	private void printCodePoint(int cp, Message m) {
		Optional<String> name = Optional.ofNullable(Character.getName(cp));
		var cpStr = Integer.toHexString(cp).toUpperCase();
		var str = Character.toString(cp);
		var utf8 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_8)).toUpperCase();
		var utf16 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_16LE)).toUpperCase();
		
		m.reply(String.format("Display: %s, Codepoint: %s, UTF-8: %s, UTF-16LE: %s%s",
				C.highlight(str),
				C.highlight("U+" + cpStr),
				C.highlight("0x" + utf8),
				C.highlight("0x" + utf16),
				name.map(s -> ", Name: " + C.highlight(s)).orElse(", UNBEKANNT")));
	}
}
