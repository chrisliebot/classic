package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;

public class RestartCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Beendet Chrisliebot und teilt dem Monitoring mit den Prozess erneut zu starten.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		invc.bot().managment().restart();
	}
}
