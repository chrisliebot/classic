package chrisliebaer.chrisliebot.util.parser;

import com.google.common.base.Preconditions;
import lombok.NonNull;

import javax.annotation.CheckReturnValue;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * This class implements a solid but limited command line parser. If offers various options for peeking and consuming token streams. This parser internally operates on
 * codepoints and offers methods to access {@link TokenSelector} instances that will convert the matching codepoints back into strings.
 * <p>
 * Great care must be taken when storing references to objects returned by this class since most calls invalidate existing objects and will lead to undefined behavior. It
 * is therefore advised to NEVER store any {@link TokenSelector} references.
 */
public class ChrislieParser {
	
	protected final int[] codepoints;
	protected int idx;
	
	private ChrislieParser parent;
	
	private ChrislieParser(ChrislieParser parser) {
		parent = parser; // store reference to parant parser
		
		idx = parser.idx;
		codepoints = parser.codepoints;
	}
	
	/**
	 * Creates a new parser from the given string.
	 *
	 * @param s The string to use.
	 */
	public ChrislieParser(@NonNull String s) {
		this(s, 0);
	}
	
	/**
	 * Creates a new parser from the given string at the given codepoint offset.
	 *
	 * @param s   The string to use.
	 * @param idx The offset in codepoints.
	 */
	public ChrislieParser(@NonNull String s, int idx) {
		codepoints = s.substring(idx).codePoints().toArray();
	}
	
	/**
	 * Creates an identical copy of this parser and stores the current parser as it's parent, allowing the user to call {@link #commit()}.
	 *
	 * @return Identical copy with it's parent set to the current instance.
	 */
	public ChrislieParser fork() {
		return new ChrislieParser(this);
	}
	
	/**
	 * Creates an {@link OptionsMap} at the current location in this parser. The option parser will not create a fork but rather work on this very instance. This is
	 * required as the option map needs to consume all encountered option so the user can advanced after the options part.
	 *
	 * @return OptionMap that will operate on this parser.
	 */
	public OptionsMap options() {
		return new OptionsMap(this);
	}
	
	/**
	 * Copies the current state of this parser into it's parent. This call is only valid if this instance was created by calling {@link #fork()}.
	 *
	 * @throws IllegalArgumentException If the parser is not a fork of an existing parser.
	 */
	public void commit() throws IllegalArgumentException {
		if (parent == null)
			throw new IllegalStateException("this parser doesn't have a parant");
		
		// update index of parent to current
		parent.idx = this.idx;
	}
	
	/**
	 * @return A selector that will match on the next codepoint.
	 * @see TokenSelector
	 */
	@CheckReturnValue
	public TokenSelector codepoint() {
		return TokenSelector.codepoint(this);
	}
	
	/**
	 * The string returned by the selector will not contain the codepoint that the predicate matched on but will still consume it.
	 *
	 * @param pred      A predicate that will be given each codepoint in the input stream until it matches at which point the consumed codepoints will be turned into a
	 *                  string and be returned to the caller.
	 * @param acceptEOF Wether the selector will also accept the end of file marker.
	 * @return A selector that will match until the given predicate is fulfilled.
	 * @see TokenSelector
	 */
	@CheckReturnValue
	public TokenSelector predicate(IntPredicate pred, boolean acceptEOF) {
		return TokenSelector.predicate(this, pred, acceptEOF);
	}
	
	/**
	 * Note that this selector will not care about quotation. It is therefore advisable to use {@link #quoted()} unless reading only a single word is strictly required.
	 *
	 * @return A selector that will match on the next sequence of non-whitespace characters.
	 */
	@CheckReturnValue
	public TokenSelector word() {
		return TokenSelector.singleWord(this);
	}
	
	/**
	 * The quoted selector behaves different, depending on various circumstances.
	 * <ul>
	 *     <li>If the current symbol is not considered a quote, the selector will behave like {@link #word()}.</li>
	 *     <li>If the current symbol is a quote, the selector will match until the same quote appears once more in unescaped form. The quote itself will be consumed.</li>
	 *     <li>While seeking for the matching quote, any escape character followed by a non quote character will be treated as a regular character and have no effect.</li>
	 *     <li>If no matching quote can be found, the parse will behave like {@link #word()}.</li>
	 * </ul>
	 *
	 * @return A selector that will match on the next world or quoted string.
	 */
	@CheckReturnValue
	public TokenSelector quoted() {
		return TokenSelector.maybeQuotedString(this);
	}
	
	/**
	 * Internal method that must be called before accessing the codepoint array to verify that there are still codepoints to read.
	 *
	 * @return {@code true} if there are still codepoints to be read.
	 */
	protected boolean canRead() {
		return idx < codepoints.length;
	}
	
	private void idx(int idx) {
		Preconditions.checkElementIndex(idx, codepoints.length);
		this.idx = idx;
	}
	
	protected void skipCodepoints(int n) {
		idx(idx + n);
	}
	
	protected int consumeCodepoint() throws ParserException {
		if (!canRead())
			throw new ParserException(this, "unexpected EOF");
		return codepoints[idx++];
	}
	
	@Override
	public String toString() {
		return "ArgumentParser{" +
				"idx=" + idx +
				", argString='" + intStreamToString(IntStream.of(codepoints)) + '\'' +
				", remaining='" + intStreamToString(IntStream.of(codepoints).skip(idx)) + '\'' +
				'}';
	}
	
	protected static String intStreamToString(IntStream in) {
		var sb = new StringBuilder();
		in.forEachOrdered(sb::appendCodePoint);
		return sb.toString();
	}
	
	/**
	 * After calling this method, the internal pointer will point to the next non-whitespace character or EOF.
	 */
	public void skipWhitespaces() {
		while (canRead() && Character.isWhitespace(codepoints[idx]))
			skipCodepoints(1);
	}
	
	public static class ParserException extends Exception { // inner class to allow access to private fields
		
		private final int[] input;
		private final int index;
		
		public ParserException(ChrislieParser parser) {
			input = parser.codepoints;
			index = parser.idx;
		}
		
		public ParserException(ChrislieParser parser, String message) {
			super(message);
			input = parser.codepoints;
			index = parser.idx;
		}
		
		public ParserException(ChrislieParser parser, String message, Throwable t) {
			super(message, t);
			input = parser.codepoints;
			index = parser.idx;
		}
		
		public ParserException(ChrislieParser parser, Throwable t) {
			super(t);
			input = parser.codepoints;
			index = parser.idx;
		}
		
		/**
		 * @return Converts the internal code point array into a string and returns what the parser saw when the exception occured.
		 */
		public String current() {
			return intStreamToString(IntStream.of(input).skip(index));
		}
		
		/**
		 * @return Reconstructs the input string from the internal code point array.
		 */
		public String string() {
			return intStreamToString(IntStream.of(input));
		}
	}
}
