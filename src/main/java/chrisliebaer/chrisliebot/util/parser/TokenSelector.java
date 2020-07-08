package chrisliebaer.chrisliebot.util.parser;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntPredicate;

/**
 * This class manages peek and consume operations on a {@link ChrislieParser} instance.
 */
@AllArgsConstructor
public class TokenSelector {
	
	private final ChrislieParser parser;
	private final TokenParser tokenParser;
	
	
	/**
	 * @return Attempts to read the token but will not actually consume the token.
	 */
	@NotNull
	public OptionalToken peek() {
		return runParser(parser.fork());
	}
	
	/**
	 * @return Attempts to read the token while consuming the input stream. Will not change state of parser if token can't be read.
	 */
	@NotNull
	public OptionalToken consume() {
		var fork = parser.fork();
		var token = runParser(fork);
		
		// only successfull parse is commited
		if (token.isSuccess())
			fork.commit();
		return token;
	}
	
	private OptionalToken runParser(ChrislieParser parser) {
		var fork = parser.fork(); // we fork, so we can put the original position into an exception
		try {
			return OptionalToken.of(tokenParser.parse(fork));
		} catch (ChrislieParser.ParserException e) {
			return OptionalToken.empty(e, fork);
		} finally {
			fork.commit(); // but we also commit back so that the callers parser is actually in sync with whatever happened during the parse code
		}
	}
	
	/**
	 * @return {@code true} if this TokenSelector is currently able to read it's token.
	 */
	public boolean canRead() {
		return peek().isSuccess();
	}
	
	protected static TokenSelector codepoint(ChrislieParser parser) {
		return new TokenSelector(parser, p -> Character.toString(p.consumeCodepoint()));
	}
	
	protected static TokenSelector singleWord(ChrislieParser parser) {
		return new TokenSelector(parser, p -> {
			if (!p.canRead())
				throw new ChrislieParser.ParserException(p, "unexpected EOF");
			
			StringBuilder sb = new StringBuilder();
			while (p.canRead()) {
				int cp = p.consumeCodepoint();
				if (Character.isWhitespace(cp)) // impossible on first loop since we skipped whitespaces and checked for EOF
					break;
				
				sb.appendCodePoint(cp);
			}
			return sb.toString();
		});
	}
	
	protected static TokenSelector maybeQuotedString(ChrislieParser parser) {
		return new TokenSelector(parser, choicepoint -> {
			var p = choicepoint.fork(); // fork in case we never match the first quote character
			if (!p.canRead())
				throw new ChrislieParser.ParserException(p, "unexpected EOF");
			
			int quote = p.consumeCodepoint();
			
			// quote parser is only called if string even starts with quote character
			if (quote == '\'' || quote == '\"') {
				StringBuilder sb = new StringBuilder();
				boolean escape = false;
				while (p.canRead()) {
					int cp = p.consumeCodepoint();
					
					// check for end of quoted string
					if (cp == quote) {
						
						// but ignore if in escape path
						if (escape) {
							escape = false;
						} else {
							// since we actually parsed the word, we need to commit our changes
							p.commit();
							return sb.toString();
						}
					}
					
					// if escaped character isn't quote character, we assume user error and add escape character to string builder
					if (escape) {
						sb.append('\\');
						escape = false;
					}
					
					// consume escape flag without adding to string builder
					if (cp == '\\') {
						escape = true;
					} else {
						sb.appendCodePoint(cp);
					}
				}
			}
			
			// since we reached this point, we were unable to find matching quote character, so we use single word parser and exit
			return choicepoint.word().consume().expect();
		});
	}
	
	protected static TokenSelector predicate(ChrislieParser parser, IntPredicate pred, boolean acceptEOF) {
		return new TokenSelector(parser, p -> {
			StringBuilder sb = new StringBuilder();
			
			try {
				while (true) {
					int cp = p.consumeCodepoint();
					
					// test for exit condition
					if (pred.test(cp))
						return sb.toString();
					
					sb.appendCodePoint(cp);
				}
			} catch (ChrislieParser.ParserException e) {
				// only cause for exception is consuming codepoints which only fails at EOF
				if (acceptEOF)
					return sb.toString();
				else
					throw e;
			}
		});
	}
	
	@FunctionalInterface
	private interface TokenParser {
		
		@NotNull
		public String parse(ChrislieParser parser) throws ChrislieParser.ParserException;
	}
}
