package chrisliebaer.chrisliebot.command.generator;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StaticGenerator implements Generator {
	
	private @NonNull String out;
	
	public StaticGenerator(@NonNull String out) {
		this.out = out;
	}
	
	public StaticGenerator(@NonNull File file) throws IOException {
		out = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
	}
	
	@Override
	public Map<String, String> generate(ChrislieListener.Invocation invc, GeneratorCommand command) throws ChrislieListener.ListenerException {
		return Map.of(Generator.DEFAULT, out);
	}
}
