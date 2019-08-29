package chrisliebaer.chrisliebot.command.flip;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;

public class FlipCommand implements ChrislieListener.Command {
	
	private static final String LOOKUP_NORMAL;
	
	private static final String LOOKUP_FLIP;
	
	static {
		String normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
				"abcdefghijklmnopqrstuvwxyz_,;.?!()[]{}<>/\\\\'" +
				"0123456789";
		String flip = "∀qϽᗡƎℲƃHIſʞ˥WNOԀὉᴚS⊥∩ΛMXʎZ" +
				"ɐqɔpǝɟbɥᴉظʞןɯuodbɹsʇnʌʍxʎz‾'؛˙¿¡)(][}{></\\\\," +
				"0ƖᄅƐㄣϛ9ㄥ86";
		
		LOOKUP_NORMAL = normal + flip;
		LOOKUP_FLIP = flip + normal;
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var arg = invc.arg();
		StringBuilder sb = new StringBuilder(arg.length());
		for (int i = 0; i < arg.length(); i++) {
			int cp = arg.codePointAt(i);
			int idx = LOOKUP_NORMAL.indexOf(cp);
			if (idx < 0)
				sb.appendCodePoint(cp);
			else
				sb.append(LOOKUP_FLIP.charAt(idx));
		}
		invc.reply("(╯°□°）╯ " + sb.reverse().toString());
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("˙ɟdoʞ ʇɥǝʇs ʇlǝM ǝᴉp");
	}
}
