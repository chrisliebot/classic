package chrisliebaer.chrisliebot.command.emojify;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class EmojifyCommand implements ChrislieListener.Command {
	
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\b +\\b", Pattern.UNICODE_CHARACTER_CLASS);
	
	private List<String> list;
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of(emojify("Wir haben so viele Emojis, wir sollten sie vielleicht auch alle nutzen, was meinst du?"));
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		try {
			var str = IOUtils.toString(getClass().getResourceAsStream("/emoji-list.txt"), StandardCharsets.UTF_8);
			list = Arrays.asList(str.split("\\r?\\n"));
		} catch (IOException e) {
			throw new ListenerException("failed to load emoji list", e);
		}
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var m = invc.arg();
		var result = emojify(m);
		if (result.length() > Message.MAX_CONTENT_LENGTH) {
			ErrorOutputBuilder.generic("Das wird leider zu lang für Discord \uD83D\uDE15").write(invc).send();
			return;
		}
		invc.reply(result);
	}
	
	private String emojify(String s) {
		var matcher = WHITESPACE_PATTERN.matcher(s);
		var sb = new StringBuilder(s.length() * 2);
		while (matcher.find()) {
			matcher.appendReplacement(sb, " " + randomEmoji() + " ");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	private String randomEmoji() {
		return list.get(ThreadLocalRandom.current().nextInt(list.size()));
	}
}
