package chrisliebaer.chrisliebot.command.string;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import lombok.NonNull;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScrambleCommand implements ChrislieListener.Command {
	
	private static final Pattern INNER_WORD_PATTERN = Pattern.compile("\\b\\w(.+?)\\w\\b");
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Command.super.help(ctx, ref);
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		invc.reply(ultimateTextMassacre(invc.arg()));
	}
	
	private static String ultimateTextMassacre(@NonNull String msg) {
		Matcher matcher = INNER_WORD_PATTERN.matcher(msg);
		return matcher.replaceAll(matchResult -> {
			String word = matchResult.group();
			
			// shuffle :)
			var list = word.codePoints().boxed().collect(Collectors.toList());
			int first = list.remove(0);
			int last = list.remove(list.size() - 1);
			Collections.shuffle(list);
			
			// reassemble string
			StringBuilder sb = new StringBuilder(word.length());
			sb.appendCodePoint(first);
			for (var c : list) {
				sb.appendCodePoint(c);
			}
			sb.appendCodePoint(last);
			
			return sb.toString();
		});
	}
}
