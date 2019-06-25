package chrisliebaer.chrisliebot.command.sed;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SedCommand implements CommandExecutor {
	
	private static final int BACKLOG_SIZE = 50;
	
	private static final Pattern SED_PATTERN = Pattern.compile("^s/(?<search>([^/]|\\\\/)+)/(?<replace>([^/]|\\\\/)*)/(?<flags>[g]*)$");
	
	// TODO: consider moving this to global state
	private CircularFifoQueue<StoredMessage> backlog = new CircularFifoQueue<>(BACKLOG_SIZE);
	
	@Override
	public synchronized void execute(Message m, String arg) {
		Preconditions.checkState(m.channel().isPresent(), "sed command invoked from private message");
		
		// we are called for every single message, so we need to store each message in the backbuffer but ignore sed invocations
		var matcher = SED_PATTERN.matcher(m.message());
		if (matcher.matches())
			doSed(m, matcher);
		else
			keepMessage(m);
	}
	
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private synchronized void doSed(Message m, Matcher matcher) {
		String channel = m.channel().get().getName();
		
		// extract groups and reverse escaped slash
		String search = matcher.group("search").replaceAll("\\\\/", "/");
		String replace = matcher.group("replace").replaceAll("\\\\/", "/");
		String flags = matcher.group("flags");
		
		Optional<StoredMessage> match = Optional.empty();
		
		// reverse loop to get last message first
		for (int i = backlog.size() - 1; i >= 0; i--) {
			var sm = backlog.get(i);
			
			// stop on first match
			if (sm.channel.equals(channel) && sm.message.contains(search)) {
				match = Optional.of(sm);
				break;
			}
		}
		
		// don't print anything if no match, user will understand
		if (match.isEmpty())
			return;
		
		var found = match.get();
		String replaced;
		
		// g: enables global replacement
		if (flags.contains("g"))
			replaced = found.message.replaceAll(search, replace);
		else
			replaced = found.message.replaceFirst(search, replace);
		
		m.reply("<" + found.nickname + "> " + replaced);
	}
	
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private synchronized void keepMessage(Message m) {
		backlog.add(new StoredMessage(m.channel().get().getName(), m.user().getNick(), m.message()));
	}
	
	@AllArgsConstructor
	private static class StoredMessage {
		
		private String channel, nickname, message;
	}
	
}
