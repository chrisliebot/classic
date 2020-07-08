package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.Optional;

public class LogConfigCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Überschreibt die aktuelle Loggerkonfiguration bis zum nächsten Neustart: <TRACE|DEBUG|INFO|WARN|ERROR> [logger-name], reset");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		
		
		// configurator is not part of public API but that's okay
		//Configurator.setRootLevel(); // TODO
		//Configurator.setLevel();
	}
	
	private void reset() {
		// TODO: find out if this also reloads it
		Configurator.reconfigure();
	}
}
