package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlainOutputImpl implements PlainOutput {
	
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\u0000\\\\]");
	
	private static final char ESCAPE_MARKER = '\0';
	private static final char ESCAPE_CHARACTER = '\\';
	
	private Function<String, String> escaper;
	private BiFunction<Object, String, String> formatResolver;
	
	private List<Supplier<String>> calls = new ArrayList<>();
	
	public PlainOutputImpl(@NonNull Function<String, String> escaper,
						   @NonNull BiFunction<Object, String, String> formatResolver) {
		this.escaper = escaper;
		this.formatResolver = formatResolver;
	}
	
	@Override
	public PlainOutput append(String s, Object... format) {
		append(() -> s, format);
		return this;
	}
	
	@Override
	public PlainOutput appendEscape(String s, Object... format) {
		appendEscape(() -> s, format);
		return this;
	}
	
	@Override
	public PlainOutput newLine() {
		calls.add(() -> "\n");
		return this;
	}
	
	@Override
	public PlainOutput clear() {
		calls.clear();
		return this;
	}
	
	protected void appendEscape(Supplier<String> supplier, Object... formats) {
		calls.add(() -> escaper.apply(applyFormats(supplier.get(), formats)));
	}
	
	protected void append(Supplier<String> supplier, Object... formats) {
		calls.add(() -> applyFormats(supplier.get(), formats));
	}
	
	private String applyFormats(String s, Object... formats) {
		for (Object format : formats) {
			s = formatResolver.apply(format, s);
		}
		return s;
	}
	
	@CheckReturnValue
	public String string() {
		return calls.stream()
				.map(Supplier::get)
				.collect(Collectors.joining());
	}
}
