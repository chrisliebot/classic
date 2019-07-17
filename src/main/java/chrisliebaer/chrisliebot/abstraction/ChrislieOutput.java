package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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
	
	public ChrislieOutput color(Color color);
	
	public ChrislieOutput color(int color);
	
	public ChrislieOutput author(String name);
	
	public ChrislieOutput authorUrl(String url);
	
	public ChrislieOutput authorIcon(String url);
	
	public ChrislieOutput field(String field, String value, boolean inline);
	
	public default ChrislieOutput field(String field, String value) {
		return field(field, value, true);
	}
	
	public @NotNull PlainOutput plain();
	
	public default ChrislieOutput plain(String s) {
		plainSimpleSet(s, plain());
		return this;
	}
	
	public PlainOutput convert();
	
	public PlainOutput replace();
	
	
	
	public void send();
 
	
	/*
	entweder
	
	* konvertierungsstring mit substitution
	* konvertierungs plain output mit [berschriebenen methoden f[r substitution
	* seperater plain output ohne subtitution, f[r loop oder so
	 */
	
	
	private static void plainSimpleSet(String s, PlainOutput plainOutput) {
		plainOutput.clear().appendEscape(s);
	}
}
