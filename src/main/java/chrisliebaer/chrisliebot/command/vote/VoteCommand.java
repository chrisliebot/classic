package chrisliebaer.chrisliebot.command.vote;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ArgumentParser;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.kitteh.irc.client.library.util.Format;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class VoteCommand implements ChrisieCommand {
	
	private static final int VOTE_TIME = 120000;
	
	private final Map<String, PendingVote> pending = new HashMap<>(); // not thread safe, external sync required
	
	@Override
	public synchronized void execute(ChrislieMessage m, String arg) {
		if (m.channel().isDirectMessage()) {
			m.reply(C.error("Abstimmungen sind nur in Chaträumen möglich."));
			return;
		}
		
		PendingVote pendingVote = pending.get(m.channel().identifier());
		
		if (pendingVote == null) {
			startVote(m, arg);
		} else {
			if ("stop".equalsIgnoreCase(arg)) {
				if (m.user().softIdentifer().equals(pendingVote.nickname()) || m.user().isAdmin()) {
					completion(pendingVote);
				} else {
					m.reply(C.error("Du bist nicht der Besitzer dieser Umfrage und kannst sie daher nicht beenden."));
				}
			} else {
				parseVote(m, arg, pendingVote);
			}
		}
	}
	
	private synchronized void startVote(ChrislieMessage m, String arg) {
		ArgumentParser parser = new ArgumentParser(arg);
		String question = parser.consumeUntil("?");
		if (question == null || question.isEmpty()) {
			m.reply(C.error("Du hast keine Frage gestellt. Eine Frage beendet man immer mit einem Fragezeichen!"));
			return;
		}
		question += parser.consume(); // eat ? from arg
		question = question.trim();
		parser.skipWhitespaces();
		
		// read options
		ArrayList<String> options = new ArrayList<>();
		while (parser.canRead()) {
			
			String option = parser.consumeUntil(",");
			if (option == null) {
				// last option, consume remaining string as option
				option = parser.consumeRemaining().trim();
			}
			
			option = option.trim();
			if (option.isEmpty()) {
				m.reply(C.error("Du hast eine leere Option angegeben."));
				return;
			}
			
			// add option
			options.add(option);
			
			// consume , and advance
			parser.consume();
			parser.skipWhitespaces();
		}
		
		if (options.isEmpty()) {
			m.reply(C.error("Du hast keine Optionen zum Abstimmen angegeben."));
			return;
		}
		
		// build output options
		StringBuilder optsOut = new StringBuilder();
		for (int i = 0; i < options.size(); i++)
			optsOut
					.append(Format.TEAL)
					.append(i + 1)
					.append(") ")
					.append(Format.RESET)
					.append(options.get(i))
					.append(" ");
		
		PendingVote pendingVote = new PendingVote()
				.nickname(m.user().softIdentifer())
				.channel(m.channel())
				.question(question)
				.options(options);
		pending.put(m.channel().identifier(), pendingVote);
		pendingVote.startVote();
		
		m.reply("Umfrage gestartet: " + C.highlight(question) + " Antwortmöglichkeiten: " + optsOut);
	}
	
	private synchronized void parseVote(ChrislieMessage m, String arg, PendingVote pendingVote) {
		try {
			int index = Integer.parseInt(arg) - 1;
			var options = pendingVote.options();
			if (index < 0 || index >= options.size()) {
				m.reply(C.error("Das ist leider keine mögliche Auswahlmöglichkeit."));
			} else {
				// count vote towards option, overriding existing vote from same user
				pendingVote.votes().put(m.user().softIdentifer(), index);
			}
		} catch (NumberFormatException e) {
			m.reply(C.error("Leider konnte ich deine Eingabe nicht verarbeiten. Bitte beachte, dass bereits eine Umfrage stattfindet."));
		}
	}
	
	@Override
	public synchronized void stop() throws Exception {
		// cancel all active votes without result
		var it = pending.values().iterator();
		while (it.hasNext()) {
			it.next().task().cancel();
			it.remove();
		}
	}
	
	private synchronized void completion(PendingVote vote) {
		// cancel task in case we got completed prematurely
		vote.task().cancel();
		
		// remove vote from pending
		pending.remove(vote.channel().identifier(), vote);
		
		// retrieve results
		List<VoteResult> results = vote.results();
		
		vote.channel().output()
				.plain("Umfrage beendet: " + vote.question() + " "
						+ results.stream()
						.map(input -> String.format("%s (%s)", input.option(), C.highlight(input.votes()))) // TODO: replace highlight format
						.collect(Collectors.joining(", "))).send();
	}
	
	@Data
	@NoArgsConstructor
	private class PendingVote {
		
		// has to be set after creation
		private TimerTask task;
		
		private @NonNull String nickname; // owning nickname
		private @NonNull ChrislieChannel channel;
		private @NonNull String question;
		private @NonNull List<String> options;
		private Map<String, Integer> votes = new HashMap<>();
		
		public List<VoteResult> results() {
			Map<Integer, VoteResult> acc = new HashMap<>(votes.size());
			
			// put all option in accumulator map
			for (int i = 0; i < options.size(); i++)
				acc.put(i, new VoteResult(options.get(i)));
			
			// count votes
			votes.values().forEach(i -> acc.get(i).inc());
			
			ArrayList<VoteResult> toplist = new ArrayList<>(acc.values());
			Collections.sort(toplist);
			return toplist;
		}
		
		public void startVote() {
			task = new TimerTask() {
				@Override
				public void run() {
					VoteCommand.this.completion(PendingVote.this);
				}
			};
			SharedResources.INSTANCE().timer().schedule(task, VOTE_TIME);
		}
	}
	
	@Data
	private static class VoteResult implements Comparable<VoteResult> {
		
		private String option;
		private int votes;
		
		public VoteResult(String option) {
			this.option = option;
		}
		
		public void inc() {
			votes++;
		}
		
		@Override
		public int compareTo(VoteResult o) {
			return o.votes - votes;
		}
	}
}
