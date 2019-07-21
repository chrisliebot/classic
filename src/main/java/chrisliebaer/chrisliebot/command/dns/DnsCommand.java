package chrisliebaer.chrisliebot.command.dns;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class DnsCommand implements ChrisieCommand {
	
	private static final int TIMEOUT = 5000;
	
	static {
		Lookup.getDefaultResolver().setTimeout(0, TIMEOUT);
	}
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		var args = arg.split(" ");
		if (args.length < 1) {
			ErrorOutputBuilder.generic("Kein Hostname angegeben.").write(m);
			return;
		}
		
		int type = Type.A;
		if (args.length >= 2)
			type = Type.value(args[1], true);
		if (type == -1) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Der DNS Typ: ")
					.appendEscape(args[1], ChrislieFormat.HIGHLIGHT)
					.appendEscape(" ist ungültig."))
					.write(m);
			return;
		}
		
		try {
			handleLookup(new Lookup(args[0], type), m, args[0]);
		} catch (TextParseException e) {
			ErrorOutputBuilder.generic("Ich erkenne da keinen Hostname. Das tut mir leid.").write(m);
		} catch (IllegalArgumentException e) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Der DNS Typ ")
					.appendEscape(args[1], ChrislieFormat.HIGHLIGHT)
					.appendEscape(" ist hier nicht erlaubt.")).write(m);
		}
	}
	
	private void handleLookup(Lookup lookup, ChrislieMessage m, String host) {
		ForkJoinPool.commonPool().execute(() -> {
			Record[] records = lookup.run();
			if (records != null) {
				Multimap<Integer, Record> mm = TreeMultimap.create();
				for (Record record : records)
					mm.put(record.getType(), record);
				
				var out = mm.asMap().entrySet().stream()
						.map(e -> Type.string(e.getKey()) + ": "
								+ e.getValue().stream()
								.map(Record::rdataToString).collect(Collectors.joining(" "))).collect(Collectors.joining(", "));
				m.reply().description().appendEscape("DNS Anfrage für ").appendEscape(host, ChrislieFormat.HIGHLIGHT).appendEscape(": ").appendEscape(out);
			} else {
				ErrorOutputBuilder.generic("Keine Antwort vom Server.").write(m);
			}
		});
	}
}
