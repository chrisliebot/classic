package chrisliebaer.chrisliebot.command.help;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.AliasSet;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HelpCommand implements ChrislieListener.Command {
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		
		String arg = invc.arg();
		
		if (arg.isBlank())
			enumerate(invc);
		else
			detail(invc, arg);
	}
	
	private void enumerate(Invocation invc) throws ListenerException {
		var ctx = invc.ctx();
		
		Map<String, Consumer<PlainOutput.JoinPlainOutput>> actions = new TreeMap<>();
		for (var ref : ctx.listeners().values()) {
			
			// skip disabled listeners
			if (ref.flexConf().isSet(FlexConf.DISPATCHER_DISABLE))
				continue;
			
			// retrieve list of exposed aliases
			var exposed = ref.aliasSet().get().values().stream()
					.filter(AliasSet.Alias::exposed)
					.map(AliasSet.Alias::name)
					.collect(Collectors.toList());
			
			// some listeners might not have any exposed commands, so we skip them
			if (exposed.isEmpty())
				continue;
			
			actions.put(exposed.get(0), desc -> desc.seperator().appendEscape(String.join("|", exposed), ChrislieFormat.HIGHLIGHT));
		}
		
		// now that we know how many (if any) commands we have, we can prepare the output
		var reply = invc.reply();
		reply.title("Befehlsliste in aktuellem Kontext");
		
		if (actions.isEmpty()) { // empty help could happen if there are not commands, or no exposed aliases (help could be called from different context)
			reply.description("Es sind keine Befehle vorhanden.");
		} else {
			var joiner = reply.description().joiner(", ");
			actions.forEach((ignore, a) -> a.accept(joiner));
			reply.footer(actions.size() + " Befehle gefunden.");
		}
		
		reply.send();
	}
	
	private void detail(Invocation invc, String alias) throws ListenerException {
		var ctx = invc.ctx();
		var maybeRef = ctx.alias(alias);
		
		// becomes true if dispatcher is disabled or value is absent
		var dispatcherDisabled = maybeRef.map(ref -> ref.flexConf().isSet(FlexConf.DISPATCHER_DISABLE)).orElse(true);
		var noVisiableAlias = maybeRef.map(ref -> ref.aliasSet().isEmpty(true)).orElse(true);
		
		if (dispatcherDisabled || noVisiableAlias) {
			ErrorOutputBuilder.generic(String.format("Ich kennen keinen Befehl mit dem Namen `%s` im aktuellen Kontext.", alias)).write(invc).send();
			return;
		}
		
		var reply = invc.reply(); // this error has nothing to do with the called listener, so we don't want to catch it's exception
		try {
			var ref = maybeRef.get();
			var command = (Command) ref.envelope().listener();
			var help = Optional.ofNullable(ref.help());
			if (help.isEmpty())
				help = command.help(ctx, ref);
			
			var exposedAliases = ref.aliasSet().get().values().stream()
					.filter(AliasSet.Alias::exposed)
					.map(AliasSet.Alias::name)
					.collect(Collectors.joining(", "));
			
			reply.title("Hilfe zu `" + alias + "`");
			reply.description(help.orElse(Optional.ofNullable(ref.help()).orElse("Zu diesem Befehl ist keine Hilfe verfügbar.")));
			reply.field("Aliases", exposedAliases);
			reply.footer("Einige versteckte Aliase werden eventuell nicht angezeigt.");
			
			reply.send();
		} catch (ListenerException e) {
			ErrorOutputBuilder.throwable(e).write(invc).send();
		}
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Erlaubt Zugriff auf eingebaute Hilfetexte von Befehlen. Listet ohne Parameter alle Befehle des aktuellen Context auf. Wird ein " +
				"Befehlsname übergeben, so wird dessen Hilfetext angezeigt.");
	}
}
