package chrisliebaer.chrisliebot.command.dns;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

//TODO: replace with async resolver
public class DnsCommand implements ChrislieListener.Command {
	
	private static final int TIMEOUT = 5000;
	
	static {
		Lookup.getDefaultResolver().setTimeout(Duration.ofMillis(TIMEOUT));
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("<Query> [<Type>]");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var args = invc.arg().split(" ");
		if (args.length < 1) {
			ErrorOutputBuilder.generic("Kein Hostname angegeben.").write(invc).send();
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
					.write(invc).send();
			return;
		}
		
		try {
			handleLookup(new Lookup(args[0], type), invc.reply(), args[0]);
		} catch (TextParseException e) {
			ErrorOutputBuilder.generic("Ich erkenne da keinen Hostname. Das tut mir leid.").write(invc).send();
		} catch (IllegalArgumentException e) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Der DNS Typ ")
					.appendEscape(args[1], ChrislieFormat.HIGHLIGHT)
					.appendEscape(" ist hier nicht erlaubt.")).write(invc).send();
		}
	}
	
	private void handleLookup(Lookup lookup, ChrislieOutput reply, String host) {
		ForkJoinPool.commonPool().execute(() -> {
			Record[] records = lookup.run();
			if (records != null) {
				Multimap<Integer, Record> mm = TreeMultimap.create();
				for (Record record : records)
					mm.put(record.getType(), record);
				
				reply.title("DNS Ergebnis");
				
				// convert results into message fields
				for (var entry : mm.asMap().entrySet()) {
					String typeStr = Type.string(entry.getKey());
					
					// convert records to strings and separate by newline
					String result = entry.getValue().stream().map(Record::rdataToString).collect(Collectors.joining("\n"));
					
					reply.field(typeStr, result);
				}
				
				// do same for string output
				reply.replace().appendEscape("DNS Anfrage für ").appendEscape(host, ChrislieFormat.HIGHLIGHT).appendEscape(": ")
						.appendEscape(mm.asMap().entrySet().stream()
								.map(e -> Type.string(e.getKey()) + ": "
										+ e.getValue().stream()
										.map(Record::rdataToString).collect(Collectors.joining(" "))).collect(Collectors.joining(", ")));
				reply.send();
			} else {
				ErrorOutputBuilder.generic("Keine Antwort vom Server.").write(reply).send();
			}
		});
	}
}
