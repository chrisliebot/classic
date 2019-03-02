package chrisliebaer.chrisliebot.command;

import com.google.common.base.Preconditions;
import lombok.NonNull;

// you are going to have a bad time if you've come here
public class ArgumentParser {
	
	private int idx;
	private String argString;
	
	@Override
	public String toString() {
		return "ArgumentParser{" +
				"idx=" + idx +
				", argString='" + argString + '\'' +
				", remaining='" + peekRemaining() + '\'' +
				'}';
	}
	
	public ArgumentParser(@NonNull String argString) {
		this(argString, 0);
	}
	
	public ArgumentParser(@NonNull String argString, int idx) {
		this.argString = argString;
		idx(idx);
	}
	
	public void idx(int idx) {
		Preconditions.checkArgument(idx >= 0, "index must not be negative");
		this.idx = idx;
	}
	
	/**
	 * After calling this method, the internal pointer will point to the next non-whitespace character.
	 */
	public void skipWhitespaces() {
		while (canRead() && Character.isWhitespace(argString.charAt(idx)))
			idx++;
	}
	
	public boolean canRead() {
		return idx < argString.length();
	}
	
	public Character peek() {
		if (!canRead())
			return null;
		
		return argString.charAt(idx);
	}
	
	public Character consume() {
		if (!canRead())
			return null;
		
		return argString.charAt(idx++);
	}
	
	public String peekRemaining() {
		if (!canRead())
			return "";
		
		return argString.substring(idx);
	}
	
	public String consumeRemaining() {
		var s = peekRemaining();
		idx = argString.length();
		return s;
	}
	
	public String peekUntil(String mark) {
		if (!canRead())
			return "";
		
		var s = peekRemaining();
		int offset = s.indexOf(mark);
		
		if (offset < 0)
			return null;
		
		return s.substring(0, offset);
	}
	
	public String consumeUntil(String mark) {
		var s = peekUntil(mark);
		if (s == null)
			return null;
		
		idx += s.length();
		return s;
	}
	
	// complicated, better implement when required
	/*
	public String peekString() {
		char c = peek();
		if (isQuote(c)) {
			return peekQuotedString(c);
		} else {
			return peekUntil(String.valueOf(c));
		}
	}
	
	private String peekQuotedString(char quote) {
		while ()
	}*/
	
	private static boolean isQuote(char c) {
		return c == '\'' || c == '"';
	}
}
