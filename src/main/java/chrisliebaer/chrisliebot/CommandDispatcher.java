package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandContainer;
import chrisliebaer.chrisliebot.config.ConfigContext;
import chrisliebaer.chrisliebot.listener.ListenerContainer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.util.Collection;
import java.util.Map;

@Slf4j
public class CommandDispatcher {
	
	private ConfigContext ctx;
	private String prefix;
	private Map<String, CommandContainer> commandTable;
	private Collection<ListenerContainer> listeners;
	
	public CommandDispatcher(@NonNull ConfigContext ctx) {
		this.ctx = ctx;
		commandTable = ctx.getCommandTable();
		prefix = ctx.botCfg().prefix();
		listeners = ctx.getChatListener();
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
		
		if (parseCommand(m))
			return;
		
		listeners.forEach(l -> l.onMessage(m));
	}
	
	public boolean parseCommand(Message m) {
		// sanitize input if user messed up
		String s = m.message().trim();
		if (!s.startsWith(prefix))
			return false;
		
		// get command name that's followed by !
		var split = s.substring(1).split(" ", 2);
		
		// extract command name but also check that user actually specified a command
		String commandName = split[0].toLowerCase().trim();
		if (commandName.isEmpty()) {
			// input was "!   " or similar, no command name given thous we ignore the message
			return false;
		}
		
		// check if argument was given or fall back to emptry string if not
		String arg = "";
		if (split.length == 2)
			arg = split[1];
		
		// check if valid command and invoke with argument
		var cmd = commandTable.get(commandName);
		if (cmd == null) {
			log.debug(C.LOG_IRC, "unkown command {}{} by {} in {}",
					prefix, commandName, m.user().getNick(), m.channel().map(Channel::getName).orElse("PRIVATE"));
			
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
}
