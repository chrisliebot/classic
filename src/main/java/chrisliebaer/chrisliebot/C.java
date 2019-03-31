package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.util.CtcpUtil;
import org.kitteh.irc.client.library.util.Format;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class C {
	
	public static final int EXIT_CODE_RESTART = 10;
	public static final int EXIT_CODE_UPGRADE = 20;
	
	private static final int MAX_LENGHT_PER_MULTILINE = 1000;
	
	public static final char[] EMPTY_CHAR_ARRAY = new char[0];
	
	public static final String MIME_TYPE_JSON = "application/json; charset=utf-8";
	
	private static final String PREFIX_ERROR = "" + Format.BOLD + Format.RED;
	private static final String PREFIX_HIGHLIGHT = "" + Format.TEAL;
	
	public static final String PERMISSION_ERROR = C.error("Befehl benötigt Adminrechte");
	
	public static final char ZERO_WIDTH_NO_BREAK_SPACE = '\uFEFF';
	public static final Marker LOG_IRC = MarkerFactory.getMarker("LOG_IRC");
	
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
	
	public static String highlight(Object s) {
		if (s == null)
			return null;
		return PREFIX_HIGHLIGHT + s + Format.RESET;
	}
	
	public static String error(Object s) {
		if (s == null)
			return null;
		return PREFIX_ERROR + "[Fehler]" + Format.RESET + " " + s + Format.RESET;
	}
	
	public static String format(String s, Format... formats) {
		return Arrays.stream(formats).map(Objects::toString).collect(Collectors.joining()) + s + Format.RESET;
	}
	
	public static String invalidChannel(@NonNull String name) {
		return C.error("Der Channel '" + C.highlight(name) + "' ist ungültig.");
	}
	
	public static String squashFormatting(String s) {
		if (s == null)
			return null;
		
		// TODO: takes the input string and attempts to reduce the amount of format codes by merging overlapping or unused definitions
		return s;
	}
	
	public static String sanitizeForSend(String msg) {
		msg = msg.replaceAll("[\n\r\u0000]", " ");
		if (msg.length() > MAX_LENGHT_PER_MULTILINE) {
			msg = msg.substring(0, MAX_LENGHT_PER_MULTILINE);
			msg += "[...]";
		}
		return msg;
	}
	
	public static void remoteConnectionError(Request req, Message m, Throwable t) {
		var reason = t.getMessage();
		m.reply(C.error(reason == null || reason.isEmpty() ?
				"Konnte remote Server nicht erreichen." :
				"Konnte remote Server nicht erreichen: " + reason));
		log.debug("request to {} failed: {}", req.url(), reason);
	}
	
	public static boolean channelSupportsFormatting(@NonNull Channel channel) {
		return channel.getModes().getByMode('c').isEmpty();
	}
	
	public static String stripHtml(String html) {
		return Jsoup.parse(html).text();
	}
	
	public static boolean isChannelOp(@NonNull Channel channel, @NonNull User user) {
		return channel.getUserModes(user).map(modes -> {
			for (ChannelUserMode mode : modes) {
				if (mode.getNickPrefix() == '@')
					return true;
			}
			return false;
		}).orElse(false);
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
	
	private C() {}
}
