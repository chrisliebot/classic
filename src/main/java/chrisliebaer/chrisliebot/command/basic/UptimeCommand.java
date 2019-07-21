package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;

public class UptimeCommand implements ChrisieCommand {
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply().description(out -> out
				.appendEscape("Ich laufe schon seit ")
				.appendEscape(C.durationToString(getRuntimeMXBean().getUptime()), ChrislieFormat.HIGHLIGHT))
				.send();
	}
}
