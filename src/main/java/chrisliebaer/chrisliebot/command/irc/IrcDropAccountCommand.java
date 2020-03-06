package chrisliebaer.chrisliebot.command.irc;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.irc.IrcMessage;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.abstraction.irc.IrcUser;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;

import java.util.Optional;

public class IrcDropAccountCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_IRC_ONLY = ErrorOutputBuilder.generic("Dieser Befehl ist nur in IRC Netzwerken verfügbar.");
	private static final ErrorOutputBuilder ERROR_BLANK = ErrorOutputBuilder.generic("Du hast keinen Zielbefehl angegeben.");
	private static final ErrorOutputBuilder ERROR_COMMAND_NOT_FOUND = ErrorOutputBuilder.generic("Der Zielbefehl existiert nicht.");
	private static final ErrorOutputBuilder ERROR_CYCLE_DETECTED = ErrorOutputBuilder.generic("Das darfst du nicht!");
	
	private ContextResolver resolver;
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Führt den nachfolgenden Befehl ohne IRC Accountinformationen aus. Hat den selben Effekt wie ein Ausloggen aus dem IRC Account.");
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.resolver = resolver;
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (!IrcService.isIrc(invc)) {
			ERROR_IRC_ONLY.write(invc).send();
			return;
		}
		
		IrcMessage msg = (IrcMessage) invc.msg();
		IrcUser user = msg.user();
		
		var arg = invc.arg();
		if (arg.isBlank()) {
			ERROR_BLANK.write(invc).send();
			return;
		}
		
		// rebuild message with new user
		user = user.asNickname();
		msg = new IrcMessage(
				msg.service(),
				user,
				msg.channel(),
				msg.message()
		);
		
		String[] args = arg.split(" ", 2);
		
		// TODO: streamline dynamic command dispatching with helper framework
		var ctx = resolver.resolve(Selector::check, msg);
		var maybeRef = ctx.alias(args[0]);
		if (maybeRef.isEmpty()) {
			ERROR_COMMAND_NOT_FOUND.write(invc).send();
			return;
		}
		
		var ref = maybeRef.get();
		var listener = (Command)ref.envelope().listener(); // since alias was resolved, we know it's a command
		
		// prevent cyclic loops
		if (this == listener) {
			ERROR_CYCLE_DETECTED.write(invc).send();
			return;
		}
		
		// build new context
		invc = new Invocation(
				invc.exceptionHandler(),
				invc.bot(),
				msg,
				ref,
				ctx,
				args.length == 1 ? "" : args[1],
				args[0]
		);
		listener.execute(invc);
	}
}
