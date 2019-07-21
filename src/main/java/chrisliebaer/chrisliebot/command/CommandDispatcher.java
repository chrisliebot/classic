package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.listener.ListenerContainer;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CommandDispatcher {
	
	private Pattern invocationPattern;
	private Map<String, CommandContainer> commandTable;
	private Collection<ListenerContainer> listeners;
	
	public CommandDispatcher(
			String prefix,
			Map<String, CommandContainer> commandTable,
			Collection<ListenerContainer> listeners) {
		this.commandTable = commandTable;
		this.listeners = listeners;
		
		invocationPattern = Pattern.compile("^" + Pattern.quote(prefix) + "(?<name>[^ ]+)( (?<argument>.+)?)?$");
	}
	
	public void dispatch(ChrislieMessage m) {
		if (checkCommand(m))
			return;
		
		listeners.forEach(l -> l.onMessage(m));
	}
	
	protected boolean checkCommand(ChrislieMessage m) {
		Optional<Invocation> invocation = parseCommand(invocationPattern.matcher(m.message()));
		return invocation.filter(i -> executeCommand(m, i)).isPresent();
	}
	
	protected Optional<Invocation> parseCommand(Matcher matcher) {
		if (matcher.matches()) {
			String name = matcher.group("name");
			
			if (name == null)
				return Optional.empty();
			
			String arg = matcher.group("argument");
			return Optional.of(new Invocation(name, arg));
		}
		return Optional.empty();
	}
	
	protected boolean executeCommand(ChrislieMessage m, Invocation invocation) {
		var commandName = invocation.name();
		var arg = invocation.args();
		
		// check if valid command and invoke with argument
		var cmd = commandTable.get(commandName);
		if (cmd == null) {
			log.debug(C.LOG_PUBLIC, "unkown command {} by {} in {}",
					commandName, m.user().displayName(), m.channel().displayName());
			
			// assume user didn't want to trigger command
			return false;
		} else {
			// check if command requires admin
			if (cmd.requireAdmin() && !m.user().isAdmin()) {
				ErrorOutputBuilder.permission().write(m);
			} else {
				// invoke command
				cmd.execute(m, arg);
			}
			
			return true;
		}
	}
	
	@Data
	private static class Invocation {
		
		public Invocation(String name, String args) {
			this.name = name.toLowerCase(); // always lowercase
			this.args = args == null ? "" : args.trim(); // never just spaces
		}
		
		private String name;
		private String args;
	}
}
