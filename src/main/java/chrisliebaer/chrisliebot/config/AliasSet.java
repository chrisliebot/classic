package chrisliebaer.chrisliebot.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public final class AliasSet implements Supplier<Map<String, AliasSet.Alias>> {
	
	private static final Pattern ALIAS_PATTERN = Pattern.compile("^(?<action>[+-]?)(?<name>[^?]+)(?<exposed>\\??)$");
	
	private Map<String, AliasState> aliases;
	private boolean replace;
	
	public AliasSet() {
		aliases = new HashMap<>(0);
		replace = false;
	}
	
	public AliasSet(AliasSet o) {
		aliases = new HashMap<>(o.aliases);
		replace = o.replace;
	}
	
	public static Optional<AliasSet> of(Collection<String> set) {
		Boolean replace = isReplace(set);
		
		if (replace == null) // set are invalid
			return Optional.empty();
		
		// store state of each alias as action lambda to perform on set
		Map<String, AliasState> aliases = new HashMap<>(set.size());
		for (String alias : set) {
			
			// parsing alias string is requied even for replacement sets since they still contain ? flag
			var matcher = ALIAS_PATTERN.matcher(alias);
			if (!matcher.matches()) // matcher can fail if empty alias name
				return Optional.empty();
			
			String name = matcher.group("name");
			var exposed = matcher.group("exposed").isBlank(); // exposed if not set
			var add = "+".equals(matcher.group("action")); // false implies "-" since sementics were already checked by "isRpleace()"
			
			// incremental alias maps could contain same alias name multiple times with different control characters
			if (aliases.containsKey(name))
				return Optional.empty();
			
			if (replace)
				aliases.put(name, new AliasState(exposed, true));
			else
				aliases.put(name, new AliasState(exposed, add));
		}
		return Optional.of(new AliasSet(aliases, replace));
	}
	
	@Override
	public Map<String, Alias> get() {
		return aliases.entrySet().stream()
				.filter(e -> e.getValue().add) // remove if add is false
				.map(e -> new Alias(e.getKey(), e.getValue().exposed))
				.collect(Collectors.toUnmodifiableMap(Alias::name, Function.identity()));
	}
	
	/**
	 * Applies the given AliasSet to this one. The will modify the current AliasSet to reflect the changes proposed in the given AliasSet. If the given AliasSet is a
	 * replacement set, it will effectively copy it's content into this one.
	 *
	 * @param o The other AliasSet.
	 */
	public void apply(AliasSet o) {
		
		// if the other set is a replacement we simply clear this set before applying changes
		if (o.replace) {
			aliases.clear();
			replace = true; // in order to maintain replacment contract, we need to taint this set as replacement
		}
		
		// we simply override existing entries with entries from the other alias set
		aliases.putAll(o.aliases);
	}
	
	// true: replacement, false: incremental, null: mixed
	private static Boolean isReplace(Collection<String> aliases) {
		
		// empty set will override existing aliases (aka clearing them)
		if (aliases.isEmpty())
			return true;
		
		Boolean replace = null;
		for (String alias : aliases) {
			boolean tmp = !(alias.startsWith("+") || alias.startsWith("-")); // + and - indicate incremental set
			if (replace == null) // first iteration decides if replacement or incremental set
				replace = tmp;
			else if (replace != tmp) // check if set changed from replacement to incremental or vice versa
				return null;
		}
		
		return replace;
	}
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString
	private static final class AliasState {
		
		@Getter private boolean exposed;
		
		private boolean add;
	}
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@ToString
	public static final class Alias {
		
		@Getter private String name;
		@Getter private boolean exposed;
	}
}
