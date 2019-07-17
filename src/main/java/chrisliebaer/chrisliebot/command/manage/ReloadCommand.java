package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.ChrisliebotIrc;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ReloadCommand implements ChrisieCommand {
	
	private ChrisliebotIrc bot;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Lade Konfiguration neu, dies kann eine Weile dauern...");
		log.info(C.LOG_PUBLIC, "reloading config, triggered by {}", m.user().displayName());
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
