package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import java.lang.management.ManagementFactory;

public class UptimeCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply("Ich laufe schon seit "
				+ C.highlight(C.durationToString(ManagementFactory.getRuntimeMXBean().getUptime())));
	}
}
