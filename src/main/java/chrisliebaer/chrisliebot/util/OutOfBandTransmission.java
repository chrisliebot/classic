package chrisliebaer.chrisliebot.util;

import org.apache.commons.io.FileUtils;

import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OutOfBandTransmission {
	
	private static final String FILE_EXTENSION = ".txt";
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	private @NotBlank String generator;
	private @NotBlank String path;
	
	public String send(String content) throws IOException {
		var uuid = UUID.randomUUID();
		var file = new File(path, uuid + ".txt");
		FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
		return generator.replace("${file}", file.getName());
	}
}
