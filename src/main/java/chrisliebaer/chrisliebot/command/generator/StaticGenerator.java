package chrisliebaer.chrisliebot.command.generator;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Map;

@AllArgsConstructor
public class StaticGenerator implements Generator {
	
	private @NonNull String out;
	
	@Override
	public Map<String, String> generate(ChrislieListener.Invocation invc, GeneratorCommand command) throws ChrislieListener.ListenerException {
		return Map.of(Generator.DEFAULT, out);
	}
}
