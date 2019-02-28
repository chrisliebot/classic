package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;

import java.lang.management.ManagementFactory;

public class UptimeCommand implements CommandExecutor {
	
	@Override
	public void execute(Message m, String arg) {
		m.reply("Ich laufe schon seit "
				+ C.highlight(C.durationToString(ManagementFactory.getRuntimeMXBean().getUptime())));
	}
}
