package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A common use case in chat bots is escaping user input in such a way that it doesn't trigger service specific commands or {@code mentions}. While generally escaping such things seems like a good idea, sometimes you need these commands. This utility class supports building strings in such a way that each segment of a string can be marked to be escaped or passed thru. Once the string has been build a callback can be specified which is given each escaped region for protocol specific escapings.
 * <p>
 * The class is build in such a way that the escaping can happen right at the end. This allows to keep building up the output over time and do the escaping at the time of sending.
 */
public abstract class PlainOutput {
	
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\u0000\\\\]");
	
	private static final char ESCAPE_MARKER = '\0';
	private static final char ESCAPE_CHARACTER = '\\';
	
	private StringBuilder builder = new StringBuilder();
	
	private Function<String, String> escaper;
	
	/**
	 * Creates a new instance of that will be using the provided escaper on calling {@link #string()}.
	 *
	 * @param escaper Function that will be called to escape strings.
	 */
	public PlainOutput(@NonNull Function<String, String> escaper) {
		this.escaper = escaper;
	}
	
	/**
	 * Appends the given String without any escaping.
	 *
	 * @param s The String to append.
	 * @return This instance for chaining.
	 */
	public PlainOutput append(String s) {
		escape(s);
		return this;
	}
	
	/**
	 * Escapes the given String before appending it.
	 *
	 * @param s The string to append.
	 * @return This instance for chaining.
	 */
	public PlainOutput appendEscape(String s) {
		if (!s.isEmpty()) {
			builder.ensureCapacity(s.length() + 2);
			builder.append(ESCAPE_MARKER);
			escape(s);
			builder.append(ESCAPE_MARKER);
		}
		return this;
	}
	
	/**
	 * Start a new line. Primarly for code readability.
	 *
	 * @return This instance for chaining.
	 */
	public PlainOutput newLine() {
		builder.append('\n');
		return this;
	}
	
	/**
	 * Takes the prepared string of this output and escapes all regions that have have been added with {@link #appendEscape(String)} using the provided {@code escaper}.
	 *
	 * @return This instance for chaining.
	 */
	protected String string() {
		StringBuilder sb = new StringBuilder(builder.length());
		
		// if set, the next character will always be appended
		boolean escaped = false;
		
		// marks start of escape region
		int start = -1;
		
		for (int i = 0; i < builder.length(); i++) {
			char c = builder.charAt(i);
			
			if (c == ESCAPE_CHARACTER && !escaped) { // start escape sequence
				escaped = true;
			} else if (c == ESCAPE_MARKER && !escaped) { // start or end escape region
				
				if (start >= 0) { // if start is set, we are calling the escape function
					sb.append(escaper.apply(builder.substring(start + 1, i)));
					start = -1;
				} else { // otherwise we mark the start
					start = i; // mark start
				}
				
			} else if (start < 0) { // only copy characters outside of escape region
				escaped = false;
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Allocation optimized escape function for the internal marker syntax that is used to identify external excape regions.
	 *
	 * @param s The string to append to the internal
	 */
	private void escape(String s) {
		var matcher = ESCAPE_PATTERN.matcher(s);
		while (matcher.find()) {
			char match = matcher.group().charAt(0);
			if (match == ESCAPE_MARKER)
				matcher.appendReplacement(builder, "\\0");
			else if (match == ESCAPE_CHARACTER)
				matcher.appendReplacement(builder, "\\\\");
		}
		matcher.appendTail(builder);
	}
}
