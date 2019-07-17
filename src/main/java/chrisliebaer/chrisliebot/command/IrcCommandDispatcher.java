package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.config.ConfigContext;
import chrisliebaer.chrisliebot.listener.ListenerContainer;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class IrcCommandDispatcher {
	
	private IrcService service;
	
	protected Pattern invocationPattern;
	protected ConfigContext ctx;
	protected Map<String, CommandContainer> commandTable;
	protected Collection<ListenerContainer> listeners;
	
	public IrcCommandDispatcher(IrcService service, @NonNull ConfigContext ctx) {
		this.service = service;
		this.ctx = ctx; // TODO: we don't need the context anymore
		commandTable = ctx.getCommandTable();
		listeners = ctx.getChatListener();
		
		invocationPattern = Pattern.compile("^" + Pattern.quote(ctx.botCfg().prefix()) + "(?<name>[^ ]+)( (?<argument>.+)?)?$");
	}
	
	private boolean isOwn(User user) {
		// ignore own messages and ignored
		return user.getClient().isUser(user) || service.ignore(user.getNick());
	}
	
	@Handler
	public void onChannelMessage(ChannelMessageEvent ev) {
		if (isOwn(ev.getActor()))
			return;
		
		var m = IrcMessage.of(service, ev);
		dispatch(m);
	}
	
	@Handler
	public void onPrivateMessage(PrivateMessageEvent ev) {
		if (isOwn(ev.getActor()))
			return;
		
		var m = IrcMessage.of(service, ev);
		dispatch(m);
	}
	
	private void dispatch(ChrislieMessage m) {
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
			// invariants of invocation instance are actually enforce here
			this.name = name.toLowerCase(); // always lowercase
			this.args = args == null ? "" : args.trim(); // never just spaces
		}
		
		private String name;
		private String args;
	}
}
