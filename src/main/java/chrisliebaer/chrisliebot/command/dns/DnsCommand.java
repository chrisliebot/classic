package chrisliebaer.chrisliebot.command.dns;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
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
			m.reply(C.error("Kein Hostname angegeben."));
			return;
		}
		
		int type = Type.A;
		if (args.length >= 2)
			type = Type.value(args[1], true);
		if (type == -1) {
			m.reply(C.error("Der DNS Typ " + C.highlight(args[1]) + " ist ungültig."));
			return;
		}
		
		try {
			handleLookup(new Lookup(args[0], type), m, args[0]);
		} catch (TextParseException e) {
			m.reply(C.error("Ich erkenne da keinen Hostname. Das tut mir leid."));
		} catch (IllegalArgumentException e) {
			m.reply(C.error("Der DNS Typ " + C.highlight(args[1]) + " ist hier nicht erlaubt."));
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
				m.reply("DNS Anfrage für " + C.highlight(host) + ": " + out);
			} else {
				m.reply(C.error("Keine Antwort vom Server."));
			}
		});
	}
}
