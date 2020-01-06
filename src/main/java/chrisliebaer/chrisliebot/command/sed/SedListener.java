package chrisliebaer.chrisliebot.command.sed;

import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier.ChannelIdentifier;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
public class SedListener implements ChrislieListener {
	
	private static final Pattern SED_PATTERN = Pattern.compile("^s/(?<search>([^/]|\\\\/)+)/(?<replace>([^/]|\\\\/)*)/(?<flags>[g]*)$");
	
	private Cache<ChannelIdentifier, CircularFifoQueue<StoredMessage>> backlog = CacheBuilder.newBuilder().build();
	
	@Override
	public void onMessage(ListenerMessage msg, boolean isCommand) throws ListenerException {
		var m = msg.msg();
		
		if (isCommand || m.channel().isDirectMessage())
			return;
		
		// we are called for every single message, so we need to store each message in the backbuffer but ignore sed invocations
		var matcher = SED_PATTERN.matcher(m.message());
		if (matcher.matches())
			try {
				doSed(msg, matcher);
			} catch (@SuppressWarnings("ProhibitedExceptionCaught") PatternSyntaxException | IndexOutOfBoundsException e) {
				ErrorOutputBuilder.throwable(e).write(msg).send();
			}
		else {
			try {
				keepMessage(msg);
			} catch (ExecutionException e) {
				throw new ListenerException(e);
			}
		}
	}
	
	private synchronized void doSed(ListenerMessage msg, Matcher matcher) throws ListenerException {
		var m = msg.msg();
		
		// compile pattern from user input
		Pattern searchPattern = Pattern.compile(matcher.group("search").replaceAll("\\\\/", "/"));
		
		// extract groups and reverse escaped slash
		String replace = matcher.group("replace").replaceAll("\\\\/", "/");
		String flags = matcher.group("flags");
		
		Optional<StoredMessage> match = Optional.empty();
		Predicate<String> searchPredicate = searchPattern.asPredicate();
		
		var queue = backlog.getIfPresent(ChannelIdentifier.of(m.channel()));
		
		if (queue == null) {
			return; // no messages stored
		}
		
		// reverse loop to get last message first
		for (int i = queue.size() - 1; i >= 0; i--) {
			var sm = queue.get(i);
			
			// stop on first match
			if (searchPredicate.test(sm.message)) {
				match = Optional.of(sm);
				break;
			}
		}
		
		// don't print anything if no match, user will understand
		if (match.isEmpty())
			return;
		
		StoredMessage found = match.get();
		Matcher searchMatcher = searchPattern.matcher(found.message);
		String replaced;
		
		// g: enables global replacement
		if (flags.contains("g"))
			replaced = searchMatcher.replaceAll(replace);
		else
			replaced = searchMatcher.replaceFirst(replace);
		
		msg.reply("<" + found.nickname + "> " + replaced);
	}
	
	private synchronized void keepMessage(ListenerMessage msg) throws ExecutionException {
		var m = msg.msg();
		var queue = backlog.get(ChannelIdentifier.of(m.channel()),
				() -> new CircularFifoQueue<>(msg.ref().flexConf().getIntegerOrFail("sed.backlog")));
		
		queue.add(new StoredMessage(m.user().displayName(), m.message()));
	}
	
	@AllArgsConstructor
	private static class StoredMessage {
		
		private String nickname, message;
	}
	
}
