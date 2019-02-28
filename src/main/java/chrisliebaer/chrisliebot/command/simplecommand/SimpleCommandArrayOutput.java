package chrisliebaer.chrisliebot.command.simplecommand;

import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SimpleCommandArrayOutput implements SimpleCommandOutput {
	
	private List<String> strings = new ArrayList<>();
	
	private SimpleCommandArrayOutput(@NonNull Collection<String> ss) {
		ss.forEach(this::addString);
	}
	
	public void addString(@NonNull String s) {
		if (!s.isEmpty())
			strings.add(s);
	}
	
	@Override
	public String out(String in) {
		int idx = new Random().nextInt(strings.size());
		return strings.get(idx);
	}
	
	public String search(String in) {
		var rng = ThreadLocalRandom.current();
		
		// fast path if no input
		if (in == null || in.isEmpty())
			return strings.get(rng.nextInt(strings.size()));
		
		Pattern pattern = Pattern.compile(in);
		
		var resultList = strings.stream()
				.filter(pattern.asPredicate())
				.collect(Collectors.toList());
		
		if (!resultList.isEmpty())
			return resultList.get(rng.nextInt(resultList.size()));
		else
			return null;
	}
	
	public static SimpleCommandArrayOutput fromCollection(@NonNull Collection<String> ss) {
		return new SimpleCommandArrayOutput(ss);
	}
	
	public static SimpleCommandArrayOutput fromFile(File f) throws IOException {
		return fromCollection(FileUtils.readLines(f, StandardCharsets.UTF_8));
	}
}
