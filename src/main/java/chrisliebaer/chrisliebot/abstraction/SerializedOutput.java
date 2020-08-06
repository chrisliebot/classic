package chrisliebaer.chrisliebot.abstraction;

import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class represents a serialized and service independent {@link ChrislieOutput} state. If this instance is given a
 * {@link ChrislieOutput} it will replicate the state that was caught in this instance. The main use case for this class
 * is to specify complex outputs in config languages without being able to call actual code.
 */
public class SerializedOutput {
	
	private String title;
	private @URL String url; // shares validity with title and permits null values
	
	private String img;
	private String thumbnail;
	
	private Integer color;
	
	private String author;
	// without the author field, these are ignored, null is also a valid value, so no need to wrap in optional
	private String authorUrl;
	private String authorIcon;
	
	private @NotNull List<Field> fields = List.of();
	
	private String footer;
	private String footerIcon;
	
	private List<PlainOutputCall> plain;
	private List<PlainOutputCall> description;
	private List<PlainOutputCall> replace;
	
	/**
	 * Calling this method will replay the serialized output onto the provided output.
	 *
	 * @param output The output to replay the state in.
	 * @return The given ChrislieOutput for method chaining.
	 */
	public ChrislieOutput apply(ChrislieOutput output) {
		return apply(output, Function.identity());
	}
	
	/**
	 * Works like {@link #apply(ChrislieOutput)} but allows the caller to register a transform that is applied to every
	 * string. The main purpose of this method is to allow parameter substitution by registering a search and replace
	 * function.
	 *
	 * @param output    The output to apply this recorded state to.
	 * @param transform A function that may or may not transform every string that's passed to the output. The function
	 *                  will not be called on null values.
	 * @return The given ChrislieOutput for method chaining.
	 */
	public ChrislieOutput apply(ChrislieOutput output, Function<String, String> transform) {
		// we don't want caller to handle null values, so instead we simply handle these with our own lambda
		Function<String, String> fn = (s) -> s == null ? null : transform.apply(s);
		
		Optional.ofNullable(title).ifPresent(title -> output.title(fn.apply(title), fn.apply(url)));
		Optional.ofNullable(img).ifPresent(img -> output.image(fn.apply(img)));
		Optional.ofNullable(thumbnail).ifPresent(thumbnail -> output.thumbnail(fn.apply(thumbnail)));
		Optional.ofNullable(color).ifPresent(output::color);
		Optional.ofNullable(author).ifPresent(author -> output.author(fn.apply(author)).authorUrl(fn.apply(authorUrl)).authorIcon(fn.apply(authorIcon)));
		Optional.ofNullable(footer).ifPresent(footer -> output.footer(fn.apply(footer), fn.apply(footerIcon)));
		
		Optional.ofNullable(plain).ifPresent(list -> applyCalls(list, output.plain(), fn));
		Optional.ofNullable(description).ifPresent(list -> applyCalls(list, output.description(), fn));
		Optional.ofNullable(replace).ifPresent(list -> applyCalls(list, output.replace(), fn));
		
		fields.forEach(field -> output.field(field.name, field.value, field.inline));
		
		return output;
	}
	
	private static void applyCalls(List<PlainOutputCall> calls, PlainOutput out, Function<String, String> fn) {
		calls.forEach(call -> call.apply(out, fn));
	}
	
	private static final class Field {
		
		private @NotBlank String name;
		private @NotBlank String value;
		private boolean inline = true;
	}
	
	private static final class PlainOutputCall {
		
		public static final ChrislieFormat[] EMPTY_FORMATS = new ChrislieFormat[0];
		
		private interface PlainMethod {
			
			public PlainOutput fn(PlainOutput output, String s, Object[] format);
		}
		
		@AllArgsConstructor
		private enum PlainOutputMethod {
			APPEND(PlainOutput::append),
			APPEND_ESCAPE(PlainOutput::appendEscape);
			
			private PlainMethod fn;
		}
		
		private @NotNull PlainOutputMethod method;
		private @NotNull String content;
		private @NotNull ChrislieFormat[] formats = EMPTY_FORMATS;
		
		public void apply(PlainOutput out, Function<String, String> fn) {
			method.fn.fn(out, fn.apply(content), formats);
		}
	}
}
