package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;

import java.util.function.Function;
import java.util.regex.Pattern;

public class PlainOutputImpl implements PlainOutput {
	
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\u0000\\\\]");
	
	private static final char ESCAPE_MARKER = '\0';
	private static final char ESCAPE_CHARACTER = '\\';
	
	private StringBuilder builder = new StringBuilder();
	
	private Function<String, String> escaper;
	
	public PlainOutputImpl(@NonNull Function<String, String> escaper) {
		this.escaper = escaper;
	}
	
	@Override
	public PlainOutput append(String s, Object... format) {
		s = applyFormats(s, format);
		escape(s);
		return this;
	}
	
	@Override
	public PlainOutput appendEscape(String s, Object... format) {
		if (!s.isEmpty()) {
			s = applyFormats(s, format);
			
			builder.ensureCapacity(s.length() + 2);
			builder.append(ESCAPE_MARKER);
			escape(s);
			builder.append(ESCAPE_MARKER);
		}
		return this;
	}
	
	@Override
	public PlainOutput newLine() {
		builder.append('\n');
		return this;
	}
	
	@Override
	public PlainOutput clear() {
		builder.setLength(0);
		return this;
	}
	
	
	public String string() {
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
	
	private static String applyFormats(String s, Object... formats) {
		// TODO: implement this
		return s;
		
		// voher muss noch ein formatierer in den konstruktor...
		
		/*
		check if irc format and apply only in irc context
		check if discord format and only apply in discord
		both discuraged
		
		check if formatter enum and resolve and apply
		etc.
		 */
		
	}
}
