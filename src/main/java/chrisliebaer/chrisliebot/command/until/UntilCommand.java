package chrisliebaer.chrisliebot.command.until;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class UntilCommand implements ChrisieCommand {
	
	private static final ErrorOutputBuilder ERROR_INVALID_DATE = ErrorOutputBuilder.generic("Ich seh da nichts was einen Zeitpunkt darstellt.");
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
	
	private Parser parser = new Parser();
	
	@Override
	public synchronized void execute(ChrislieMessage m, String arg) {
		long now = System.currentTimeMillis();
		
		List<DateGroup> parse = parser.parse(arg);
		var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
		
		if (dates.isEmpty()) {
			ERROR_INVALID_DATE.write(m);
			return;
		}
		
		Date date = dates.get(0);
		long diff = date.getTime() - now;
		
		String pre = diff < 0 ? "war vor" : "ist in";
		
		m.reply().description(out -> out
				.appendEscape("Das Datum ").appendEscape(DATE_FORMAT.format(date), ChrislieFormat.HIGHLIGHT)
				.appendEscape(" ").appendEscape(pre).appendEscape(" ").appendEscape(C.durationToString(diff), ChrislieFormat.HIGHLIGHT)).send();
	}
}
