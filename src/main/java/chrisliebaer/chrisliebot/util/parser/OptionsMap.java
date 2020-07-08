package chrisliebaer.chrisliebot.util.parser;

import lombok.NonNull;
import org.apache.commons.collections4.map.HashedMap;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This is a POSIX style command line argument parser. An attempt was made to distinguish between options without an argument (here called flags) and regular options that
 * require an argument. Additionally, each option or flag may only appear exactly once as this is still a chat bot that doesn't require ffmpeg like levels of command line
 * fuckery.
 * <p>
 * Most command line parsers operate in two phases: first you define your flags and option, then you execute the parser. After that you have to access a returned object
 * with the parsed values. This requires you to specificy your options at least two times and makes the code clunky to use. This interface first collects all possible
 * flags and options and gives you a {@link Supplier} that acts as a reference to the value, once the parser has been triggered. After that the parser can be started and
 * the suppliers be accessed. Accessing any returned Supplier before calling {@link #parse()} will cause a {@link NoSuchElementException} to be thrown.
 * <p>
 * Flags and options are created by calling the {@link #flag(String...)} method for flags, or the {@link #option(Function, String...)} for options. Short versions are
 * automatically recognized by their single character string and handled appropriately.
 */
@SuppressWarnings("ReturnOfInnerClass") // caller is expected to throw values away after command call finished
public class OptionsMap {
	
	private final ChrislieParser parser;
	
	
	private boolean parsed; // used to indicate that parse() has been called
	private Map<String, OptionContainer> map = new HashedMap<>();
	
	
	/**
	 * Creates a new OptionsMap from the given {@link ChrislieParser}. The OptionsMap will start consuming tokens whatever position the given {@link ChrislieParser} is
	 * currently at, when {@link #parse()} is called.
	 *
	 * @param parser The parser that this OptionsMap will operate on.
	 */
	protected OptionsMap(@NonNull ChrislieParser parser) {
		this.parser = parser;
	}
	
	/**
	 * Registers a new boolean flag. This is an on/off toggle. A boolean flag is either present or absent and can carry no value.
	 *
	 * @param flags A list of short and long names for this flag. None of the names must overlap with other flags or options.
	 * @return A {@link BooleanSupplier} that can be access after the {@link #parse()} method has been called.
	 * @throws IllegalArgumentException If the given flags collide with existing flags or options.
	 */
	public BooleanSupplier flag(String... flags) throws IllegalArgumentException {
		// check if flags are causing conflict
		for (var f : flags)
			if (map.containsKey(f))
				throw new IllegalArgumentException("duplicated key: " + f + " in " + Arrays.toString(flags));
		
		// construct flag instance and map to given flags
		var flag = new Flag();
		for (var f : flags)
			map.put(f, flag);
		
		return flag;
	}
	
	/**
	 * Registers a new option. An option always specifies a value that is following the option itself.
	 *
	 * @param selector The selector to use for parsing this options values.
	 * @param options  A list of short and long names for this option. None of the names must overlap with other flags or options.
	 * @return A {@link Supplier} that can be used to access the parsed value after the {@link #parse()} method has been called.
	 * @throws IllegalArgumentException If the given options collide with existing flags or options.
	 */
	public Supplier<Optional<String>> option(Function<ChrislieParser, TokenSelector> selector, String... options) throws IllegalArgumentException {
		// check if flags are causing conflict
		for (var f : options)
			if (map.containsKey(f))
				throw new IllegalArgumentException("duplicated options: " + f + " in " + Arrays.toString(options));
		
		// construct option instance and map to given options
		var option = new Option(selector);
		for (var o : options)
			map.put(o, option);
		
		return option;
	}
	
	/**
	 * Needs to be called after all flags and options have been registered.
	 */
	public void parse() throws ChrislieParser.ParserException {
		
		// we keep a fork of the current parser that we commit after we have completed parsing
		var parser = this.parser.fork();
		
		// loops until there is no dash character leading the next non-whitespace input
		while (true) {
			parser.skipWhitespaces();
			
			var fork = parser.fork();
			// at this point we expect new option or assume we have parsed all of them
			if (!fork.canRead() || fork.consumeCodepoint() != '-')
				break;
			
			/* The usage of the fork instance is a bit ticky, but any other solution would require more complex logic
			 * fork is used to basically unwind the consumption of the second dash. So if instead a short option is detected, we don't commit
			 */
			fork.commit();
			
			// second dash would indicate long-option, while non dash indicates beginning of short option(group)
			if (fork.codepoint().consume().expect("second dash or short option").startsWith("-")) {
				
				// pass remainder to long-option parser
				parseLong(fork);
				
				fork.commit();
			} else {
				
				// short-option, current symbol is short option, pass directly to parser function
				while (parseShort(parser)) {
					
					// current symbol being whitespace indicates that flag group ended
					if (!parser.canRead() || parser.codepoint().peek().expect().isBlank()) {
						break;
					}
				}
			}
		}
		
		// commit changes to parser
		parser.commit();
		
		// set parser flag to allow accessing parsed values in inner class
		parsed = true;
	}
	
	/**
	 * In posix command line arguments, flags can be group together like in {@code tar -czvf archive.tar.gz}. Note how the last option, namely {@code f} is actually an
	 * option with a value. This is permitted as long as no other short codes follow.
	 *
	 * @param parser The parser instance to use for parsing the short code.
	 * @return {@code true} if the parsed short code is part of a flag that carries no value. If the parsed option has a value attached {@code false} is returned.
	 * @throws ChrislieParser.ParserException If the short code is unkown or EOF is reached.
	 */
	private boolean parseShort(ChrislieParser parser) throws ChrislieParser.ParserException {
		var fork = parser.fork(); // fork so we can accurately point to parsing error
		String c = fork.codepoint().consume().expect("short option name");
		var v = map.get(c);
		if (v == null)
			throw new ChrislieParser.ParserException(parser, "invalid short option `%s`".formatted(c));
		
		// commit and skip whitespaces, but only commit skipped whitespaces if option turns out to have value (see if) at end of method
		fork.commit();
		fork.skipWhitespaces();
		
		try {
			v.visit(fork);
		} catch (OptionsMapException e) {
			throw new ChrislieParser.ParserException(parser, "failed to parse short option `%s`: `%s`".formatted(c, e.getMessage()), e);
		}
		
		// this implements behavior according to javadoc
		if (v instanceof Option) {
			// if v is not option, we effectively roll back the consume whitespaces
			fork.commit();
			return false;
		}
		else {
			return true;
		}
	}
	
	private void parseLong(ChrislieParser parser) throws ChrislieParser.ParserException {
		var fork = parser.fork();
		
		final boolean[] isEqual = {false}; // dirty hack to find out if option ended with equal sign
		String name = fork.predicate(cp -> {
			if (cp == '=')
				isEqual[0] = true;
			return Character.isWhitespace(cp) || cp == '=';
		}, true).consume().expect("long option name");
		
		var v = map.get(name);
		if (v == null)
			throw new ChrislieParser.ParserException(parser, "unkown long option name `%s`".formatted(name));
		
		// if option name was termined by whitespace, we skip additional whitespaces
		if (!isEqual[0])
			fork.skipWhitespaces();
		
		try {
			v.visit(fork);
		} catch (OptionsMapException e) {
			throw new ChrislieParser.ParserException(parser, "failed to parse long option `%s`: `%s`".formatted(name, e.getMessage()), e);
		}
		fork.commit();
	}
	
	private interface OptionContainer {
		
		public void visit(ChrislieParser parser) throws OptionsMapException, ChrislieParser.ParserException;
	}
	
	private class Flag implements BooleanSupplier, OptionContainer {
		
		private boolean value;
		
		@Override
		public boolean getAsBoolean() {
			if (!OptionsMap.this.parsed)
				throw new NoSuchElementException("you must call parse() before accessing this value");
			return value;
		}
		
		@Override
		public void visit(ChrislieParser parser) throws OptionsMapException {
			if (value)
				throw new OptionsMapException("flag has already been set");
			
			value = true;
		}
	}
	
	private class Option implements Supplier<Optional<String>>, OptionContainer {
		
		private final Function<ChrislieParser, TokenSelector> selector;
		private Optional<String> value = Optional.empty();
		
		public Option(Function<ChrislieParser, TokenSelector> selector) {
			this.selector = selector;
		}
		
		@Override
		public Optional<String> get() {
			if (!OptionsMap.this.parsed)
				throw new NoSuchElementException("you must call parse() before accessing this value");
			return value;
		}
		
		@Override
		public void visit(ChrislieParser parser) throws ChrislieParser.ParserException, OptionsMapException {
			if (value.isPresent())
				throw new OptionsMapException("option has already been set");
			
			value = Optional.of(selector.apply(parser).consume().expect("option value"));
		}
	}
	
	
	public static class OptionsMapException extends Exception {
		
		public OptionsMapException(String reason) {
			super(reason);
		}
	}
}
