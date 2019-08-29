package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

// TODO: replace marker feature with list of function calls?, could make functions protected and substitution could use protected methods directly, also backslash escape bug
public class PlainOutputImpl implements PlainOutput {
	
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\u0000\\\\]");
	
	private static final char ESCAPE_MARKER = '\0';
	private static final char ESCAPE_CHARACTER = '\\';
	
	private StringBuilder builder = new StringBuilder();
	
	private Function<String, String> escaper;
	private BiFunction<Object, String, String> formatResolver;
	
	public PlainOutputImpl(@NonNull Function<String, String> escaper,
						   @NonNull BiFunction<Object, String, String> formatResolver) {
		this.escaper = escaper;
		this.formatResolver = formatResolver;
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
		
		// keeps track of current state
		boolean escaped = false, escapeRegion = false;
		
		// remember content of escape region
		StringBuilder tmp = new StringBuilder();
		
		for (int i = 0; i < builder.length(); i++) {
			char c = builder.charAt(i);
			
			if (c == ESCAPE_CHARACTER && !escaped) { // start escape sequence
				escaped = true;
			} else if (c == ESCAPE_MARKER && !escaped) { // start or end escape region
				if (escapeRegion) {
					// leaving escape region
					escapeRegion = false;
					
					// append escaped content and reset temporary string builder
					sb.append(escaper.apply(tmp.toString()));
					tmp.setLength(0);
				} else {
					// entering escape region
					escapeRegion = true;
				}
			} else {
				escaped = false; // always reset escape flag
				
				if (escapeRegion)
					tmp.append(c);
				else
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
				matcher.appendReplacement(builder, "\\\\\\\\");
		}
		matcher.appendTail(builder);
	}
	
	private String applyFormats(String s, Object... formats) {
		for (Object format : formats) {
			s = formatResolver.apply(format, s);
		}
		return s;
	}
}
