package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * This interface is a core component of the output service abstraction. The methods of this interface act as a superset
 * for the features of all services.
 */
public interface ChrislieOutput {
	
	public ChrislieOutput title(String title, String url);
	
	public default ChrislieOutput title(String title) {
		return title(title, null);
	}
	
	public ChrislieOutput image(String url);
	
	public ChrislieOutput thumbnail(String url);
	
	public @NonNull PlainOutput description();
	
	public default ChrislieOutput description(String s) {
		plainSimpleSet(s, description());
		return this;
	}
	
	public default ChrislieOutput description(Consumer<PlainOutput> out) {
		out.accept(description());
		return this;
	}
	
	public ChrislieOutput color(Color color);
	
	public ChrislieOutput color(int color);
	
	public ChrislieOutput author(String name);
	
	public ChrislieOutput authorUrl(String url);
	
	public ChrislieOutput authorIcon(String url);
	
	public ChrislieOutput field(String field, String value, boolean inline);
	
	public default ChrislieOutput field(String field, String value) {
		return field(field, value, true);
	}
	
	public ChrislieOutput footer(String text, String iconUrl);
	
	public default ChrislieOutput footer(String text) {
		return footer(text, null);
	}
	
	public PlainOutput plain();
	
	public default ChrislieOutput plain(String s) {
		plainSimpleSet(s, plain());
		return this;
	}
	
	public default ChrislieOutput plain(Consumer<PlainOutput> out) {
		out.accept(plain());
		return this;
	}
	
	public PlainOutput.PlainOutputSubstituion convert();
	
	public default ChrislieOutput convert(String s) {
		convert().clear().appendEscapeSub(s);
		return this;
	}
	
	public default ChrislieOutput convert(Consumer<PlainOutput.PlainOutputSubstituion> out) {
		out.accept(convert());
		return this;
	}
	
	public PlainOutput replace();
	
	public default ChrislieOutput replace(String s) {
		plainSimpleSet(s, replace());
		return this;
	}
	
	public default ChrislieOutput replace(Consumer<PlainOutput> out) {
		out.accept(replace());
		return this;
	}
	
	public void send(); // TODO: implement callback for message transmission
	
	private static void plainSimpleSet(String s, PlainOutput plainOutput) {
		plainOutput.clear().appendEscape(s);
	}
}
