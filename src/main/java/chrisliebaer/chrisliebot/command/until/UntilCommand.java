package chrisliebaer.chrisliebot.command.until;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO: port to new v3 architecture
public class UntilCommand implements ChrislieListener.Command {
	
	private static final ErrorOutputBuilder ERROR_INVALID_DATE = ErrorOutputBuilder.generic("Ich seh da nichts was einen Zeitpunkt darstellt.");
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
	
	private Parser parser = new Parser();
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Hiermit kann der Datumsparser für die Timer getestet werden.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		long now = System.currentTimeMillis();
		
		List<DateGroup> parse = parser.parse(invc.arg());
		var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
		
		if (dates.isEmpty()) {
			ERROR_INVALID_DATE.write(invc).send();
			return;
		}
		
		Date date = dates.get(0);
		long diff = date.getTime() - now;
		
		String pre = diff < 0 ? "war vor" : "ist in";
		
		invc.reply().description(out -> out
				.appendEscape("Das Datum ").appendEscape(DATE_FORMAT.format(date), ChrislieFormat.HIGHLIGHT)
				.appendEscape(" ").appendEscape(pre).appendEscape(" ").appendEscape(C.durationToString(diff), ChrislieFormat.HIGHLIGHT)).send();
	}
}
