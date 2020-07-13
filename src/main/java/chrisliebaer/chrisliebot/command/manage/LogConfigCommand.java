package chrisliebaer.chrisliebot.command.manage;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.parser.ChrislieParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.HashSet;
import java.util.Optional;

@Slf4j
public class LogConfigCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Überschreibt die aktuelle Loggerkonfiguration bis zum nächsten Neustart: <TRACE|DEBUG|INFO|WARN|ERROR> [logger-name.[*]], reset");
	}
	
	@SneakyThrows // TODO: remove once error output builder was reworked
	@Override
	public void execute(Invocation invc) throws ListenerException {
		ChrislieParser parser = new ChrislieParser(invc.arg());
		
		var mode = parser.word(true).consume().expect("mode");
		if ("reset".equalsIgnoreCase(mode)) {
			reset();
			invc.reply("Logger Konfiguration wurde neu geladen");
			return;
		}
		
		var level = switch (mode.toLowerCase()) {
			case "trace" -> Level.TRACE;
			case "debug" -> Level.DEBUG;
			case "info" -> Level.INFO;
			case "warn" -> Level.WARN;
			case "error" -> Level.ERROR;
			default -> throw new ListenerException("implement new error throwing");
		};
		
		// consume all loger names and collectively update their level
		var names = new HashSet<String>();
		while(parser.word(true).canRead())
			names.add(parser.word(true).consume().expect());
		
		if (names.isEmpty()) {
			// only update root logger
			Configurator.setRootLevel(level);
			invc.reply().title("Root Loger wurde auf `%s` gesetzt".formatted(level)).send();
		} else {
			var reply = invc.reply().title("Logger wurden auf `%s` gesetzt".formatted(level));
			var desc = reply.description().joiner(", ");
			for (var name : names) {
				desc.seperator();
				desc.appendEscape(name);
				if (name.endsWith("*")) {
					name = name.substring(0, name.length() - 1);
					Configurator.setAllLevels(name, level);
				} else {
					Configurator.setLevel(name, level);
				}
			}
			reply.send();
		}
	}
	
	private void reset() {
		// TODO: find out if this also reloads it
		Configurator.reconfigure();
	}
}
