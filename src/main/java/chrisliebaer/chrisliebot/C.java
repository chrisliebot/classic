package chrisliebaer.chrisliebot;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.util.CtcpUtil;
import org.kitteh.irc.client.library.util.Format;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public final class C {
	
	public static final int EXIT_CODE_RESTART = 10;
	public static final int EXIT_CODE_UPGRADE = 20;
	
	public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
	
	private static final int MAX_LENGHT_PER_MULTILINE = 1000;
	
	public static final char[] EMPTY_CHAR_ARRAY = new char[0];
	
	public static final String MIME_TYPE_JSON = "application/json; charset=utf-8";
	
	public static final char ZERO_WIDTH_NO_BREAK_SPACE = '\uFEFF';
	
	public static final String UA_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36";
	
	public static String escapeStrSubstitution(String s) {
		return s.replaceAll("\\$\\{", "\\$\\${");
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T unsafeCast(Object value) {
		return (T) value;
	}
	
	public static void sendChannelMessage(Channel channel, String s) {
		if (s == null || s.isEmpty())
			return;
		
		// strip formatting if channel doesn't support it
		if (!C.channelSupportsFormatting(channel))
			s = Format.stripAll(s);
		
		if (!CtcpUtil.isCtcp(s))
			s = C.ZERO_WIDTH_NO_BREAK_SPACE + s;
		
		channel.sendMultiLineMessage(C.sanitizeForSend(C.escapeNickname(channel, s)));
	}
	
	public static String squashFormatting(String s) {
		if (s == null)
			return null;
		
		// TODO: takes the input string and attempts to reduce the amount of format codes by merging overlapping or unused definitions
		return s;
	}
	
	public static boolean isLongParseable(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public static String sanitizeForSend(String msg) {
		msg = msg.replaceAll("[\n\r\u0000]", " ");
		if (msg.length() > MAX_LENGHT_PER_MULTILINE) {
			msg = msg.substring(0, MAX_LENGHT_PER_MULTILINE);
			msg += "[...]";
		}
		return msg;
	}
	
	public static boolean channelSupportsFormatting(@NonNull Channel channel) {
		return channel.getModes().getByMode('c').isEmpty();
	}
	
	public static String stripHtml(String html) {
		return Jsoup.parse(html).text();
	}
	
	public static String escapeNickname(Channel channel, String s) {
		Pattern nickPattern = Pattern.compile(
				channel.getNicknames().stream().map(Pattern::quote).collect(Collectors.joining("|")),
				Pattern.CASE_INSENSITIVE);
		return nickPattern.matcher(s).replaceAll(m -> C.escapeNickname(m.group()));
	}
	
	public static String escapeNickname(@NonNull String nickname) {
		if (nickname.length() <= 1)
			return nickname;
		
		return inject(nickname, 1, ZERO_WIDTH_NO_BREAK_SPACE);
	}
	
	@SuppressWarnings("MagicNumber")
	public static String durationToString(long s) {
		s = Math.abs(s);
		
		// SirYwell hauen wenn kaputt
		s /= 1000;
		long days = s / (24 * 3600);
		s %= 24 * 3600;
		long hours = s / 3600;
		s %= 3600;
		long minutes = s / 60;
		long seconds = s % 60;
		
		String daysStr = "";
		if (days == 1)
			daysStr = "einem Tag";
		else if (days > 1)
			daysStr = days + " Tage";
		
		String hoursStr = "";
		if (hours == 1)
			hoursStr = "einer Stunde";
		else if (hours > 1)
			hoursStr = hours + " Stunden";
		
		String minutesStr = "";
		if (minutes == 1)
			minutesStr = "einer Minute";
		else if (minutes > 1)
			minutesStr = String.format("%02d Minuten", minutes);
		
		String secondsStr = "";
		if (seconds == 1)
			secondsStr = "einer Sekunde";
		else if (seconds > 1)
			secondsStr = String.format("%02d Sekunden", seconds);
		
		String[] strs = {daysStr, hoursStr, minutesStr, secondsStr};
		return Arrays.stream(strs).filter(StringUtils::isNoneBlank).collect(Collectors.joining(" "));
	}
	
	private static String inject(String in, int pos, char c) {
		return in.substring(0, pos) + c + in.substring(pos);
	}
	
	public static Optional<DayOfWeek> stringToDay(@NonNull String day) {
		day = day.trim().toLowerCase();
		
		switch (day) {
			case "montag":
			case "mon":
			case "mo":
			case "monday":
				return Optional.of(DayOfWeek.MONDAY);
			case "dienstag":
			case "di":
			case "tuesday":
			case "tue":
			case "tues":
			case "tu":
				return Optional.of(DayOfWeek.TUESDAY);
			case "mittwoch":
			case "mi":
			case "mit":
			case "mitt":
			case "mittw":
			case "wednesday":
			case "wed":
				return Optional.of(DayOfWeek.WEDNESDAY);
			case "donnerstag":
			case "do":
			case "donn":
			case "thursday":
			case "th":
			case "thu":
			case "thur":
			case "thurs":
				return Optional.of(DayOfWeek.THURSDAY);
			case "freitag":
			case "fr":
			case "fri":
			case "friday":
				return Optional.of(DayOfWeek.FRIDAY);
			case "samstag":
			case "sonnabend":
			case "sa":
			case "sam":
			case "sams":
			case "saturday":
			case "sat":
				return Optional.of(DayOfWeek.SATURDAY);
			case "sonntag":
			case "so":
			case "sonn":
			case "sunday":
			case "su":
			case "sun":
				return Optional.of(DayOfWeek.SUNDAY);
		}
		
		return Optional.empty();
	}
}
