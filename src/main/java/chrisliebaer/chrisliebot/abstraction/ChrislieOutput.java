package chrisliebaer.chrisliebot.abstraction;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface ChrislieOutput {
	
	public ChrislieOutput title(String title, String url);
	
	public ChrislieOutput image(String url);
	
	public ChrislieOutput thumbnail(String url);
	
	public ChrislieOutput description(String description);
	
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
	
	public void send();
}
