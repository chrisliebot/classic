package chrisliebaer.chrisliebot.command.help;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.command.CommandContainer;
import com.google.common.collect.TreeMultimap;

import java.util.Map;
import java.util.stream.Collectors;

public class HelpCommand implements ChrisieCommand {
	
	private TreeMultimap<String, String> aliasMap = TreeMultimap.create();
	private Map<String, String> bindings;
	private Map<String, CommandContainer> cmdDefs;
	
	public HelpCommand(Map<String, String> bindings, Map<String, CommandContainer> cmdDefs) {
		this.bindings = bindings;
		this.cmdDefs = cmdDefs;
	}
	
	@Override
	public void start() throws Exception {
		// create inverse mapping
		bindings.forEach((visible, cmdDef) -> aliasMap.put(cmdDef, visible));
	}
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		if (arg.isEmpty()) {
			// TODO: filter admin commands if no admin, or just build second list
			
			String list = aliasMap.asMap().values().stream()
					.map(s -> String.join("|", s))
					.map(C::highlight)
					.collect(Collectors.joining(", "));
			
			m.reply("Befehlsliste: " + list);
		} else {
			String cmdName = bindings.get(arg);
			if (cmdName == null)
				m.reply(C.error("Dieser Befehl ist mir nicht bekannt."));
			else
				m.reply("Hilfetext zu " + C.highlight(arg) + ": " + cmdDefs.get(cmdName).help());
		}
	}
}
