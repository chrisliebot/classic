package chrisliebaer.chrisliebot.command.flip;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

public class FlipCommand implements ChrisieCommand {
	
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
	public void execute(ChrislieMessage m, String arg) {
		StringBuilder sb = new StringBuilder(arg.length());
		for (int i = 0; i < arg.length(); i++) {
			int cp = arg.codePointAt(i);
			int idx = LOOKUP_NORMAL.indexOf(cp);
			if (idx < 0)
				sb.appendCodePoint(cp);
			else
				sb.append(LOOKUP_FLIP.charAt(idx));
		}
		m.reply("(╯°□°）╯ " + sb.reverse().toString());
	}
}
