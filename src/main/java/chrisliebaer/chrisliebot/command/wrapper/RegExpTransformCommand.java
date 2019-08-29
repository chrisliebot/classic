package chrisliebaer.chrisliebot.command.wrapper;

import chrisliebaer.chrisliebot.config.flex.FlexConf;

public class RegExpTransformCommand extends TransformCommand {
	
	@Override
	protected String transformArg(String arg, FlexConf flex) throws ListenerException {
		var regex = flex.getStringOrFail("regexp.regex");
		var replace = flex.getStringOrFail("regexp.replace");
		return arg.replaceFirst(regex, replace);
	}
}
