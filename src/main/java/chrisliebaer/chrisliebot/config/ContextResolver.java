package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.scope.ScopeMapping;
import chrisliebaer.chrisliebot.config.scope.Selector;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ContextResolver {
	
	@Getter private List<ScopeMapping> mappings;
	@Getter private Map<String, ChrislieGroup> groups;
	@Getter private Set<ChrislieListener.Envelope> envelopes;
	
	public ContextResolver(@NonNull List<ScopeMapping> mappings) {
		this.mappings = Collections.unmodifiableList(mappings);
		
		// extract all groups from mapping (remember that group names are unique)
		groups = new HashMap<>();
		mappings.stream()
				.map(ScopeMapping::groups)
				.flatMap(List::stream)
				.filter(g -> !groups.containsKey(g.name())) // only visit each group once
				.flatMap(ContextResolver::flattenGroups)
				.forEach(group -> groups.put(group.name(), group));
		
		groups = Collections.unmodifiableMap(groups); // make unmodifiable
		
		// extract all listeners from all groups (identity is only way to make sure we got each listener exactly once)
		envelopes = Collections.newSetFromMap(new IdentityHashMap<>());
		groups.values().stream()
				.map(ChrislieGroup::refs)
				.map(Map::values)
				.flatMap(Collection::stream)
				.map(ListenerReference::envelope)
				.forEach(envelopes::add);
		envelopes = Collections.unmodifiableSet(envelopes);
	}
	
	public <T> ChrislieContext resolve(BiFunction<Selector, T, Boolean> lookup, T t) {
		ChrislieContext ctx = new ChrislieContext();
		
		for (ScopeMapping mapping : mappings)
			if (mapping.checkAll(lookup, t))
				ctx.addGroups(mapping.groups());
		
		return ctx;
	}
	
	private static Stream<ChrislieGroup> flattenGroups(ChrislieGroup group) {
		return Stream.concat(Stream.of(group), group.includes().stream().flatMap(ContextResolver::flattenGroups));
	}
}
