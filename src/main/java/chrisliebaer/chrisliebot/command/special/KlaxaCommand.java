package chrisliebaer.chrisliebot.command.special;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

public class KlaxaCommand implements ChrisieCommand {
	
	private static final String KLAXA_ACCOUNT = "klaxa";
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		var id = m.user().identifier();
		if (id.isPresent() && KLAXA_ACCOUNT.equals(id.get()))
			m.reply("Guten Abend!");
		else
			m.reply("Guten Morgen klaxa!");
	}
}
