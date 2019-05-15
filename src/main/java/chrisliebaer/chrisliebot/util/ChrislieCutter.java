package chrisliebaer.chrisliebot.util;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.irc.client.library.util.Cutter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChrislieCutter implements Cutter {
	
	private static final int LIMIT_CORRECTION = -10;
	
	@Override
	public @NonNull List<String> split(@NonNull String message, @NonNegative int limit) {
		// limit is know to be bugged, so we simply reduce it by a fixed amount of characters and hope for the best
		limit += LIMIT_CORRECTION;
		
		ArrayList<String> out = new ArrayList<>(1); // assume one liner
		
		ArrayDeque<String> remain = new ArrayDeque<>(Arrays.asList(message.split(" ")));
		
		StringBuilder sb = new StringBuilder();
		
		while (!remain.isEmpty()) {
			
			// check if adding string would exceed limit
			if (remain.peekFirst().length() + sb.length() + 1 > limit) { // adding one for space
				
				// force split string if stringbuffer is empty
				if (sb.length() == 0) {
					String s = remain.removeFirst();
					sb.append(s, 0, limit + 1);
					
					// push remaining string back to stack
					remain.addFirst(s.substring(limit + 1));
					
					// continue operation
					continue;
				}
				
				// commit current string buffer to output
				out.add(sb.toString());
				sb = new StringBuilder();
			} else {
				// append current string to string builder
				if (sb.length() != 0)
					sb.append(' ');
				
				sb.append(remain.removeFirst());
			}
		}
		
		// append pending string builer, if not empty
		if (sb.length() != 0)
			out.add(sb.toString());
		
		return out;
	}
}
