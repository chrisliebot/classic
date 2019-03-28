package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.abstraction.Message;
import lombok.NonNull;
import org.kitteh.irc.client.library.Client;

/**
 * Seperates command execution logic from meta data such as help text.
 */
public class CommandContainer implements CommandExecutor {
	
	private static final String NO_HELP_AVAILABLE = "Für diesen Befehl ist keine Hilfe verfügbar.";
	
	private String help;
	private CommandExecutor executor;
	
	
	public CommandContainer(@NonNull CommandExecutor executor, String help) {
		this.help = help;
		this.executor = executor;
	}
	
	public String help() {
		if (help == null || help.isEmpty())
			return NO_HELP_AVAILABLE;
		return help;
	}
	
	@Override
	public void init(Client client) throws Exception {
		executor.init(client);
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
	public void execute(Message m, String arg) {
		executor.execute(m, arg);
	}
	
	@Override
	public boolean requireAdmin() {
		return executor.requireAdmin();
	}
}
