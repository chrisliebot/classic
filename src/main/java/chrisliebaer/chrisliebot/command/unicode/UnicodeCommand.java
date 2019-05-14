package chrisliebaer.chrisliebot.command.unicode;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

public class UnicodeCommand implements CommandExecutor {
	
	private static final Pattern CODEPOINT_INPUT = Pattern.compile("^U\\+(?<cp>[0-9A-Fa-f]{1,8})$");
	
	@Override
	public void execute(Message m, String arg) {
		arg = arg.trim();
		
		int cp;
		var matcher = CODEPOINT_INPUT.matcher(arg);
		if (matcher.matches()) {
			try {
				cp = Integer.parseInt(matcher.group("cp"), 16);
			} catch (NumberFormatException e) {
				m.reply(C.error("Das sah zwar gut aus, aber ich habs trotzdem nicht gerafft."));
				return;
			}
		} else {
			cp = arg.codePointAt(0);
		}
		
		
		if (!Character.isValidCodePoint(cp)) {
			m.reply(C.error("Das ist kein gültiger Codepoint."));
			return;
		}
		
		Optional<String> name = Optional.ofNullable(Character.getName(cp));
		var cpStr = Integer.toHexString(cp).toUpperCase();
		var str = Character.toString(cp);
		var utf8 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_8)).toUpperCase();
		var utf16 = Hex.encodeHexString(str.getBytes(StandardCharsets.UTF_16LE)).toUpperCase();
		
		m.reply(String.format("Eingabe: %s, Codepoint: U+%s, UTF-8: 0x%s, UTF-16LE: 0x%s%s",
				str,
				cpStr,
				utf8,
				utf16,
				name.map(s -> ", Name: " + s).orElse(", UNBEKANNT")));
	}
}
