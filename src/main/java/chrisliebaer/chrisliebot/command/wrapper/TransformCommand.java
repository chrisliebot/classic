package chrisliebaer.chrisliebot.command.wrapper;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.flex.FlexConf;

import java.util.Optional;

public class TransformCommand implements ChrislieListener.Command {
	
	private static final String TRANSFORM_TARGET = "transform.target";
	
	// if set will merge this tansforms flex config with the target, allowing to override the target flex conf, otherwise the target flex conf is used
	private static final String TRANSFORM_REDFINE_FLEX = "transform.flex";
	
	private ListenerReference target(ChrislieContext ctx, FlexConf flex) throws ListenerException {
		var target = flex.getStringOrFail(TRANSFORM_TARGET);
		var ref = ctx.listener(target)
				.orElseThrow(() -> new ListenerException(String.format("unable to locate listener `%s` in current context", target)));
		if (!(ref.envelope().listener() instanceof Command))
			throw new ListenerException(String.format("target listener `%s` does not implement command", target));
		return ref;
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		var target = target(ctx, ref.flexConf());
		
		var help = Optional.ofNullable(target.help());
		if (help.isEmpty()) {
			var listener = (Command) target.envelope().listener();
			help = listener.help(ctx, target);
		}
		
		return help;
	}
	
	protected String transformArg(String arg, FlexConf flex) throws ListenerException {
		return arg;
	}
	
	@Override
	public final void execute(Invocation invc) throws ListenerException {
		var flex = invc.ref().flexConf();
		var arg = transformArg(invc.arg(), flex);
		
		ListenerReference target = target(invc.ctx(), flex);
		Command command = (Command) target.envelope().listener();
		
		/* right now we only have our own flex conf, if we were to call a command that has it's own flex conf, we would be missing these values
		 * likewise, if we simply pass the targets flex conf into the invocation, we have no way to redefine flex conf values
		 * therefore we first have to locate the targets flex confg, then (optionally) merge it with our own
		 */
		var targetFlex = target.flexConf();
		
		// lookup happens in our own flex conf
		if (flex.isSet(TRANSFORM_REDFINE_FLEX))
			targetFlex.apply(flex); // puts this transforms flex conf first, overriding defined values
		
		command.execute(invc.arg(arg).ref(target));
	}
}
