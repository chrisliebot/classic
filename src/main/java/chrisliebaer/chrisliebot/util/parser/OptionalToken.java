package chrisliebaer.chrisliebot.util.parser;

import chrisliebaer.chrisliebot.util.parser.ChrislieParser.ParserException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Interface that represents a token parse attempt. Can either be {@link ValueToken} or {@link NoToken}.
 */
public interface OptionalToken {
	
	/**
	 * @return {@code true} if the token was successfully parsed.
	 */
	public boolean isSuccess();
	
	/**
	 * @return Wraps the value of this class into an {@link Optional}.
	 */
	public Optional<String> optional();
	
	/**
	 * @return The exception that was raised why parsing the token. Only valid if parsing failed.
	 * @throws NoSuchElementException If the token was successfully parsed and thous no exception was raised.
	 */
	public ParserException throwable() throws NoSuchElementException;
	
	/**
	 * Unwraps the value, throwing an exception, if the value is absent.
	 *
	 * @return The parsed token.
	 * @throws ParserException The exception that was raised while parsing the token.
	 */
	public String expect() throws ParserException;
	
	/**
	 * Unwraps the value, throwing an exception with, if the value is absent. The message will be used to create a new
	 * exception, describing on a higher level what the caller expects to parse in case the value is absent.
	 *
	 * @param msg A message describing on an high level, what token the caller expect to receive. Such as "filepath",
	 *            "username" or similar.
	 * @return The parsed token.
	 * @throws ParserException An exception, combining the parsing error with the expected value provided by the caller
	 *                         via the message parameter.
	 */
	public String expect(String msg) throws ParserException;
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ValueToken implements OptionalToken {
		
		private final String value;
		
		@Override
		public boolean isSuccess() {
			return true;
		}
		
		@Override
		public Optional<String> optional() {
			return Optional.of(value);
		}
		
		@Override
		public ParserException throwable() throws NoSuchElementException {
			throw new NoSuchElementException("token was successfully parsed so there is no throwable attached");
		}
		
		@Override
		public String expect() throws ParserException {
			return value;
		}
		
		@Override
		public String expect(String msg) throws ParserException {
			return value;
		}
	}
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class NoToken implements OptionalToken {
		
		@Getter private final ParserException throwable;
		private final ChrislieParser parser;
		
		@Override
		public boolean isSuccess() {
			return false;
		}
		
		@Override
		public Optional<String> optional() {
			return Optional.empty();
		}
		
		@Override
		public String expect() throws ParserException {
			throw new ParserException(parser, throwable);
		}
		
		@Override
		public String expect(String msg) throws ParserException {
			throw new ParserException(parser, "expected `%s` but %s".formatted(msg, throwable.getMessage()), throwable);
		}
	}
	
	public static OptionalToken of(String value) {
		return new ValueToken(value);
	}
	
	public static OptionalToken empty(ParserException throwable, ChrislieParser parser) {
		return new NoToken(throwable, parser);
	}
}
