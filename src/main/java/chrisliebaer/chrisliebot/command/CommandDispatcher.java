package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.config.ConfigContext;
import chrisliebaer.chrisliebot.listener.ListenerContainer;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CommandDispatcher {
	
	protected Pattern invocationPattern;
	protected ConfigContext ctx;
	protected Map<String, CommandContainer> commandTable;
	protected Collection<ListenerContainer> listeners;
	
	public CommandDispatcher(@NonNull ConfigContext ctx) {
		this.ctx = ctx;
		commandTable = ctx.getCommandTable();
		listeners = ctx.getChatListener();
		
		invocationPattern = Pattern.compile("^" + Pattern.quote(ctx.botCfg().prefix()) + "(?<name>[^ ]+)( (?<argument>.+)?)?$");
	}
	
	@Handler
	public void onChannelMessage(ChannelMessageEvent ev) {
		var m = Message.of(ev, ctx);
		dispatch(m);
	}
	
	@Handler
	public void onPrivateMessage(PrivateMessageEvent ev) {
		var m = Message.of(ev, ctx);
		dispatch(m);
	}
	
	private void dispatch(Message m) {
		// ignore own messages and ignored
		if (m.getClient().isUser(m.user()) || ctx.botCfg().ignore().contains(m.user().getNick()))
			return;
		
		if (checkCommand(m))
			return;
		
		listeners.forEach(l -> l.onMessage(m));
	}
	
	protected boolean checkCommand(Message m) {
		Optional<Invocation> invocation = parseCommand(invocationPattern.matcher(m.message()));
		return invocation.isEmpty() || executeCommand(m, invocation.get());
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
	
	protected boolean executeCommand(Message m, Invocation invocation) {
		var commandName = invocation.name();
		var arg = invocation.args();
		
		// check if valid command and invoke with argument
		var cmd = commandTable.get(commandName);
		if (cmd == null) {
			log.debug(C.LOG_IRC, "unkown command {} by {} in {}",
					commandName, m.user().getNick(), m.channel().map(Channel::getName).orElse("PRIVATE"));
			
			// assume user didn't want to trigger command
			return false;
		} else {
			// check if command requires admin
			if (cmd.requireAdmin() && !ctx.isAdmin(m.user())) {
				m.reply(C.PERMISSION_ERROR);
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
	
	public static class AprilFoolsDayDispatcher extends CommandDispatcher {
		
		private static final Pattern APRIL_FOOLSDAY_INVOCATION = Pattern.compile("^(?<name>[^ ]+).js( (?<argument>.+)?)?$");
		
		public AprilFoolsDayDispatcher(@NonNull ConfigContext ctx) {
			super(ctx);
		}
		
		@Override
		protected boolean checkCommand(Message m) {
			LocalDate now = LocalDate.now();
			if (now.getMonth() != Month.APRIL || now.getDayOfMonth() != 1)
				return super.checkCommand(m);
			
			Optional<Invocation> invocation = parseCommand(APRIL_FOOLSDAY_INVOCATION.matcher(m.message()));
			
			if (invocation.isPresent())
				return executeCommand(m, invocation.get());
			
			invocation = parseCommand(invocationPattern.matcher(m.message()));
			if (invocation.isEmpty() || !commandTable.containsKey(invocation.get().name))
				return false;
			
			var inv = invocation.get();
			m.reply(C.error("Ich wurde in JavaScript mit Node.js® neu geschrieben. Der alte Prefix wurde abgeschafft und" +
					" alle Befehle müssen nun mit " + C.highlight(".js")) + " enden. JavaScript gehört die Zukunft! Nieder mit C und C++!");
			m.reply("Beispiel: " + inv.name() + ".js " + inv.args());
			
			// return true since we don't actually want to have this trigger listeners
			return true;
		}
	}
}
