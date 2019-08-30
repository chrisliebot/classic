package chrisliebaer.chrisliebot.command.help;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.command.CommandContainer;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.Map;

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
			var reply = m.reply();
			var description = reply.description();
			reply.title("Befehlsliste");
			
			boolean follow = false;
			for (Collection<String> s : aliasMap.asMap().values()) {
				if (follow)
					description.appendEscape(", ");
				
				String aliasGroup = String.join("|", s);
				description.appendEscape(aliasGroup, ChrislieFormat.HIGHLIGHT);
				
				
				follow = true;
			}
			
			reply.send();
		} else {
			String cmdName = bindings.get(arg);
			
			if (cmdName == null)
				ErrorOutputBuilder.generic("Dieser Befehl ist mir nicht bekannt.").write(m);
			else {
				var reply = m.reply();
				var help = cmdDefs.get(cmdName).help();
				
				reply.title("Hilfetext zu " + arg);
				reply.description(help);
				
				reply.replace()
						.appendEscape("Hilfetext zu ").appendEscape(cmdName, ChrislieFormat.HIGHLIGHT)
						.appendEscape(": ").appendEscape(help);
				
				reply.send();
			}
		}
	}
}
