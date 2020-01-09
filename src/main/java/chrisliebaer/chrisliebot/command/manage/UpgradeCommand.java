package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;

import java.util.Optional;

public class UpgradeCommand implements ChrislieListener.Command { // TODO: replace with implementation that first attempts to build and run chrisliebot before upgrading
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Beendet Chrisliebot und teilt dem Monitoring mit den Prozess Chrisliebot zu upgraden.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		invc.bot().managment().upgrade();
	}
}
