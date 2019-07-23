package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutputSubstituionImpl;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput.PlainOutputSubstituion;
import chrisliebaer.chrisliebot.abstraction.PlainOutputImpl;
import lombok.NonNull;
import org.apache.commons.lang.text.StrLookup;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IrcOutput implements ChrislieOutput {
	
	// stores values that are passed to methods we can't resolve directly
	private HashMap<String, String> map = new HashMap<>(10), fields = new HashMap<>(0);
	
	// will receive all messages on sending
	private Consumer<String> sink;
	
	// used for escaping strings inside PlainOutput instances
	private Function<String, String> escaper;
	private PlainOutputImpl plain, description;
	
	// strategy for converting received calls into irc message (default is to use title and description/plain)
	private Supplier<String> converter = new Supplier<>() {
		@Override
		public String get() {
			String out = "";
			var title = map.get("title");
			if (title != null && !title.isBlank()) {
				out += title;
			}
			
			var descriptionStr = description.string();
			if (!descriptionStr.isBlank()) {
				
				// append spacing to seperate title from plain
				if (!out.isEmpty())
					out += ": ";
				
				out += descriptionStr;
			}
			
			// fall back to plain text if empty
			if (out.isEmpty())
				out = plain.string();
			
			return out;
		}
	};
	
	public IrcOutput(@NonNull Function<String, String> escaper, @NonNull Consumer<String> sink) {
		this.escaper = escaper;
		this.sink = sink;
		plain = new PlainOutputImpl(escaper, IrcFormatter::format);
		description = new PlainOutputImpl(escaper, IrcFormatter::format);
	}
	
	@Override
	public IrcOutput title(String title, String url) {
		map.put("title", title);
		map.put("titleUrl", url);
		return this;
	}
	
	@Override
	public IrcOutput image(String url) {
		map.put("imageUrl", url);
		return this;
	}
	
	@Override
	public IrcOutput thumbnail(String url) {
		map.put("thumbnailUrl", url);
		return this;
	}
	
	@Override
	public IrcOutput color(Color color) {
		// there really is nothing we could to with a color
		return this;
	}
	
	@Override
	public IrcOutput color(int color) {
		// there really is nothing we could to with a color
		return this;
	}
	
	@Override
	public IrcOutput author(String name) {
		map.put("author", name);
		return this;
	}
	
	@Override
	public IrcOutput authorUrl(String url) {
		map.put("authorUrl", url);
		return this;
	}
	
	@Override
	public IrcOutput authorIcon(String url) {
		map.put("authorIcon", url);
		return this;
	}
	
	@Override
	public IrcOutput field(String field, String value, boolean inline) {
		fields.put(field, value);
		return this;
	}
	
	@Override
	public IrcOutput footer(String text, String iconUrl) {
		map.put("footerText", text);
		map.put("footerIconUrl", iconUrl);
		return this;
	}
	
	@Override
	public @NotNull PlainOutput plain() {
		return plain;
	}
	
	@Override
	public PlainOutput description() {
		return description;
	}
	
	@Override
	public PlainOutputSubstituion convert() {
		// swap strategy to using this output with substitutions from gathered method calls
		PlainOutputSubstituionImpl substitution = new PlainOutputSubstituionImpl(escaper, IrcFormatter::format, new StrLookup() {
			@Override
			public String lookup(String key) {
				switch (key) {
					case "plain":
						return plain.string();
					case "description":
						return description.string();
					default:
						if (key.startsWith("f-")) {
							return fields.getOrDefault(key.substring(2), "MISSING_KEY(" + key + ")");
						} else {
							return map.getOrDefault(key, "MISSING_KEY(" + key + ")");
						}
				}
			}
		});
		converter = substitution::string;
		return substitution;
	}
	
	@Override
	public PlainOutput replace() {
		PlainOutputImpl output = new PlainOutputImpl(escaper, IrcFormatter::format);
		converter = output::string;
		return output;
	}
	
	@Override
	public void send() {
		sink.accept(converter.get());
	}
}
