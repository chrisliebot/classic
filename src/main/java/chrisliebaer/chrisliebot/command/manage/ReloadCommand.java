package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReloadCommand implements CommandExecutor {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Lade Konfiguration neu, dies kann eine Weile dauern...");
		bot.doReload()
				.thenAccept(v -> m.reply("Konfiguration wurde erfolgreich neu geladen."))
				.exceptionally(throwable -> {
					m.reply(C.error("Konfiguration konnte nicht erneut geladen werden." +
							" Zustand des Dirty Flags: " + bot.dirty())
							+ " Grund: " + throwable.getMessage());
					return null;
				});
	}
	
	@Override
	public boolean requireAdmin() {
		return true;
	}
}
