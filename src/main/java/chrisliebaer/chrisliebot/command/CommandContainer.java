package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import lombok.NonNull;

/**
 * Seperates command execution logic from meta data such as help text.
 */
public class CommandContainer implements ChrisieCommand {
	
	private static final String NO_HELP_AVAILABLE = "Für diesen Befehl ist keine Hilfe verfügbar.";
	
	private String help;
	private ChrisieCommand executor;
	
	
	public CommandContainer(@NonNull ChrisieCommand executor, String help) {
		this.help = help;
		this.executor = executor;
	}
	
	public String help() {
		if (help == null || help.isEmpty())
			return NO_HELP_AVAILABLE;
		return help;
	}
	
	@Override
	public void init(ChrislieService service) throws Exception {
		executor.init(service);
	}
	
	@Override
	public void start() throws Exception {
		executor.start();
	}
	
	@Override
	public void stop() throws Exception {
		executor.stop();
	}
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		executor.execute(m, arg);
	}
	
	@Override
	public boolean requireAdmin() {
		return executor.requireAdmin();
	}
}
