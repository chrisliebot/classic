package chrisliebaer.chrisliebot;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32C;

@Slf4j
@UtilityClass
public final class C {
	
	public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
	
	public static final String MIME_TYPE_JSON = "application/json; charset=utf-8";
	
	public static final char ZERO_WIDTH_NO_BREAK_SPACE = '\uFEFF';
	
	public static final String UA_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36";
	
	public static String escapeStrSubstitution(String s) {
		return s.replaceAll("\\$\\{", "\\$\\${");
	}
	
	/**
	 * Casts whatever you have into whatever you want. It's like magic but like the really dark kind of magic that
	 * usually backfires pretty hard.
	 *
	 * @param value The value to cast, duh.
	 * @param <T>   The target type to cast into.
	 * @return The same as value, but casted into whatever type you wanted.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unsafeCast(Object value) {
		return (T) value;
	}
	
	public static String format(Duration duration) {
		String s = "";
		
		var days = duration.toDaysPart();
		if (days != 0)
			s += days + "d";
		
		var hours = duration.toHoursPart();
		if (hours != 0)
			s += hours + "h";
		
		var minutes = duration.toMinutesPart();
		if (minutes != 0)
			s += minutes + "m";
		
		var seconds = duration.toSecondsPart();
		if (seconds != 0)
			s += seconds + "s";
		
		if (s.isBlank())
			s = "jetzt";
		
		return s;
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
	
	public static String stripHtml(String html) {
		if (html == null)
			return null;
		return Jsoup.parse(html).text();
	}
	
	// TODO: remove after until command has been ported to java 8 Duration
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
	
	public static Optional<DayOfWeek> stringToDay(@NonNull String day) {
		day = day.trim().toLowerCase();
		
		return switch (day) {
			case "montag", "mon", "mo", "monday" -> Optional.of(DayOfWeek.MONDAY);
			case "dienstag", "di", "tuesday", "tue", "tues", "tu" -> Optional.of(DayOfWeek.TUESDAY);
			case "mittwoch", "mi", "mit", "mitt", "mittw", "wednesday", "wed" -> Optional.of(DayOfWeek.WEDNESDAY);
			case "donnerstag", "do", "donn", "thursday", "th", "thu", "thur", "thurs" -> Optional.of(DayOfWeek.THURSDAY);
			case "freitag", "fr", "fri", "friday" -> Optional.of(DayOfWeek.FRIDAY);
			case "samstag", "sonnabend", "sa", "sam", "sams", "saturday", "sat" -> Optional.of(DayOfWeek.SATURDAY);
			case "sonntag", "so", "sonn", "sunday", "su", "sun" -> Optional.of(DayOfWeek.SUNDAY);
			default -> Optional.empty();
		};
		
	}
	
	/**
	 * Generates visually pleasing colors by hasing the given input und deriving a deterministic color value.
	 *
	 * @param bytes An array of data that will be fed into the hash function.
	 * @return Deterministic color based on the input value.
	 */
	public Color hashColor(byte[] bytes) {
		CRC32C crc = new CRC32C();
		crc.update(bytes);
		
		// convert hash value into fraction [0,1)
		double f = Math.abs((int) crc.getValue());
		f /= Integer.MAX_VALUE;
		return new Color(Color.HSBtoRGB((float) f, 1f, 1f));
	}
}
