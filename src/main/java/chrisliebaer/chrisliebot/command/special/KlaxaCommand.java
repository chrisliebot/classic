package chrisliebaer.chrisliebot.command.special;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

public class KlaxaCommand implements CommandExecutor {
	
	private static final String KLAXA_ACCOUNT = "klaxa";
	
	@Override
	public void execute(Message m, String arg) {
		if (KLAXA_ACCOUNT.equals(m.user().getAccount().orElse(null)))
			m.reply("Guten Abend!");
		else
			m.reply("Guten Morgen klaxa!");
	}
}
