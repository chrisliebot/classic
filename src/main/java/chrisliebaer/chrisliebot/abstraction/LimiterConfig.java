package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.util.OutOfBandTransmission;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.element.MessageReceiver;
import org.kitteh.irc.client.library.util.CtcpUtil;
import org.kitteh.irc.client.library.util.Format;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class LimiterConfig {
	
	private static final String FLEX_OFFSET = "limitercfg.offset";
	private static final String FLEX_MAX_LINES = "limitercfg.maxLines";
	private static final String FLEX_STRIP_LINEBREAK = "limitercfg.stripLinebreak";
	private static final String FLEX_CUT_NOTICE = "limitercfg.cutNotice";
	private static final String FLEX_OUT_OF_BAND = "limitercfg.oob";
	private static final String FLEX_OUT_OF_BAND_DISABLE = "limitercfg.oob.disable";
	private static final String FLEX_STRIP_IRC_FORMATTING = "limitercfg.stripIrc";
	
	// offset will be added to the size parameter of split method
	private int offset;
	
	// limit of lines to be printed, any overflowing line (regardless of reason) will be dropped
	private int maxLines;
	
	// remove all explicit linebreaks from the input string
	private boolean stripLineBreak;
	
	// if true, the size of the cut content will be stated after the last output line
	private boolean appendCutNotice;
	
	// handle to out of band transmission instance, setting this value will enable out of band transmission for messages that would otherwise be limited
	private OutOfBandTransmission outOfBand;
	
	// strip all irc formatting codes from output
	private boolean stripIrcFormatting;
	
	public static LimiterConfig of(FlexConf flex) throws ChrislieListener.ListenerException {
		var cfg = new LimiterConfig();
		cfg.offset = flex.getInteger(FLEX_OFFSET).orElse(0);
		cfg.maxLines = flex.getIntegerOrFail(FLEX_MAX_LINES);
		cfg.stripLineBreak = flex.isSet(FLEX_STRIP_LINEBREAK);
		cfg.appendCutNotice = flex.isSet(FLEX_CUT_NOTICE);
		cfg.stripIrcFormatting = flex.isSet(FLEX_STRIP_IRC_FORMATTING);
		
		if (!flex.isSet(FLEX_OUT_OF_BAND_DISABLE))
			cfg.outOfBand = flex.get(FLEX_OUT_OF_BAND, OutOfBandTransmission.class).orElse(null);
		
		return cfg;
	}
	
	public LimiterConfig send(MessageReceiver receiver, String message) {
		
		// remove illegal characters
		message = message.replace("\0", "");
		
		if (stripLineBreak)
			message = C.NEWLINE_PATTERN.matcher(message).replaceAll(" ");
		
		receiver.sendMultiLineMessage(message, this::split);
		
		return this;
	}
	
	public List<String> split(String message, int limit) {
		// limit is broken when connected to znc, a hard coded offset should fix that
		limit += offset;
		
		if (stripIrcFormatting)
			message = Format.stripAll(message);
		
		// regular messages are prefixed with special byte to prevent accidental triggering of other automated services
		String prefix = "";
		if (!CtcpUtil.isCtcp(message)) {
			prefix = String.valueOf(C.ZERO_WIDTH_NO_BREAK_SPACE);
			
			// when prefix is set, it will reduce the line capacity, so we need to adjust our limit accordingly
			limit -= prefix.length();
		}
		
		Preconditions.checkArgument(limit > 0, "limit must be greater 0 or impossible to find solution");
		
		List<String> out = new ArrayList<>((message.length() / limit) + 1); // make educated guess to prevent reallocation in most cases
		
		// each remaining newline requires processing the following characters as a seperate line, so we split the input at this level and process line by line
		for (var line : C.NEWLINE_PATTERN.split(message)) {
			ArrayDeque<String> remain = new ArrayDeque<>(Arrays.asList(line.split(" ")));
			StringBuilder sb = new StringBuilder();
			
			while (!remain.isEmpty()) {
				
				// check if adding string would exceed limit
				if (remain.peekFirst().length() + sb.length() + 1 > limit) { // adding one for space
					
					// force split string if stringbuffer is empty, this happens if next word is too big even when on a single line
					if (sb.length() == 0) {
						String s = remain.removeFirst();
						sb.append(s, 0, limit + 1);
						
						// push remaining string back to stack
						remain.addFirst(s.substring(limit + 1));
						
						// continue operation
						continue;
					}
					
					// commit current string buffer to output
					out.add(prefix + sb.toString());
					sb.setLength(0);
				} else {
					// append current string to string builder
					if (sb.length() != 0)
						sb.append(' ');
					
					sb.append(remain.removeFirst());
				}
			}
			
			// append pending string builer, if not empty
			if (sb.length() != 0)
				out.add(prefix + sb.toString());
		}
		
		// we need to check if the output exceeds our limits and take appropriate action
		int excess = out.size() - maxLines;
		if (excess > 0) {
			
			if (outOfBand == null) {
				// drop excess lines
				out = out.subList(0, maxLines);
				
				if (appendCutNotice) {
					String notice = String.format("(%s Zeile(n) wurden abgeschnitten.)", excess);
					
					// while highly unlikely, it might be possible so we need to check
					if (notice.length() <= limit) {
						
						// if the notice would exceed the last lines limit, we append a new line...
						var last = out.get(out.size() - 1);
						if (last.length() + notice.length() + 1 > limit) { // +1 for space
							out.add(notice);
						} else { // ...otherwise we append to the last line
							out.set(out.size() - 1, last + " " + notice);
						}
					}
				}
			} else {
				try {
					String url = outOfBand.send(Format.stripAll(message));
					out = List.of("Die Ausgabe war zu lang und wurde umgeleitet: " + url);
				} catch (IOException e) {
					log.error("failed to write out of band message, falling back to in-band output", e);
				}
			}
		}
		
		return out;
	}
}
