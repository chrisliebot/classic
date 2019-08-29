package chrisliebaer.chrisliebot.command.generator;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import com.google.common.base.Charsets;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileGenerator implements Generator {
	
	private boolean search;
	private File file;
	
	public FileGenerator(@NonNull Config cfg) throws ChrislieListener.ListenerException {
		search = cfg.search;
		file = new File(cfg.file);
		
		if (!file.canRead())
			throw new ChrislieListener.ListenerException("unable to locate file: " + file);
	}
	
	@Override
	public Map<String, String> generate(ChrislieListener.Invocation invc, GeneratorCommand command) throws ChrislieListener.ListenerException {
		try {
			var arg = invc.arg();
			var lines = FileUtils.readLines(file, Charsets.UTF_8);
			
			if (search && !arg.isEmpty()) {
				var pred = Pattern.compile(arg).asPredicate().negate();
				lines.removeIf(pred);
			}
			
			if (lines.isEmpty())
				return null;
			
			var idx = ThreadLocalRandom.current().nextInt(lines.size());
			return Map.of(Generator.DEFAULT, lines.get(idx));
			
		} catch (IOException e) {
			throw new ChrislieListener.ListenerException("failed to open generator file", e);
		} catch (PatternSyntaxException ignore) {
			return null; // we simply return no result
		}
	}
	
	protected static class Config {
		
		private @NotBlank String file;
		private boolean search;
	}
}
