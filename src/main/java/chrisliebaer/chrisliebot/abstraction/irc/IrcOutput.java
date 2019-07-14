package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IrcOutput implements ChrislieOutput {
	
	private PlainOutput output;
	private Consumer<String> consumer;
	private Supplier<String> string;
	
	public IrcOutput(Function<String, String> escaper, Consumer<String> consumer) {
		output = new PlainOutput(escaper) {
			{
				string = this::string;
			}
		};
		this.consumer = consumer;
	}
	
	@Override
	public IrcOutput title(String title, String url) {
		return this;
	}
	
	@Override
	public IrcOutput image(String url) {
		return this;
	}
	
	@Override
	public IrcOutput thumbnail(String url) {
		return this;
	}
	
	@Override
	public IrcOutput description(String description) {
		return this;
	}
	
	@Override
	public IrcOutput color(Color color) {
		return this;
	}
	
	@Override
	public IrcOutput color(int color) {
		return this;
	}
	
	@Override
	public IrcOutput author(String name) {
		return this;
	}
	
	@Override
	public IrcOutput authorUrl(String url) {
		return this;
	}
	
	@Override
	public IrcOutput authorIcon(String url) {
		return this;
	}
	
	@Override
	public IrcOutput field(String field, String value, boolean inline) {
		return this;
	}
	
	@Override
	public @NotNull PlainOutput plain() {
		return output;
	}
	
	@Override
	public void send() {
		consumer.accept(string.get());
	}
}
