package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * A context contains the resulting set of commands, listeners and flex config values after checking an entity against all scopes.
 */
@Slf4j
public class ChrislieContext {
	
	private Map<String, ListenerContext> listeners = new HashMap<>();
	private Map<String, ListenerContext> aliases = new HashMap<>();
	
	private Set<ChrislieGroup> included = Collections.newSetFromMap(new IdentityHashMap<>());
	
	@Getter private FlexConf flexConf = new FlexConf();
	
	/**
	 * Creates an empty context.
	 */
	public ChrislieContext() {}
	
	public ChrislieContext(Collection<ChrislieGroup> groups) {
		addGroups(groups);
	}
	
	public void addGroups(Collection<ChrislieGroup> groups) {
		groups.forEach(this::addGroup);
	}
	
	public void addGroup(ChrislieGroup group) {
		if (included.contains(group)) {
			log.trace("ignoring include of group `{}` since it was already included before", group.name());
			return;
		}
		
		// included groups go first
		group.includes().forEach(this::addGroup);
		
		log.trace("adding group `{}`", group.name());
		
		// merging flex conf of group so that most recently added group provides values first
		flexConf.apply(group.flexConf());
		
		// lookup existing listener context and merge with each ref in group
		for (var e : group.refs().entrySet()) {
			var ctx = listeners.get(e.getKey());
			
			// the new context will actually be unsuable due to it's defaults, but since it will be merged in the following lines, it's fine
			if (ctx == null) {
				ctx = new ListenerContext();
				listeners.put(e.getKey(), ctx);
			}
			ctx.apply(e.getValue());
		}
		
		updateAliases();
		
		// remember included group
		included.add(group);
	}
	
	private void updateAliases() {
		aliases.clear();
		for (ListenerContext ctx : listeners.values()) {
			for (AliasSet.Alias alias : ctx.aliasSet().get().values()) {
				var old = aliases.put(alias.name(), ctx);
				if (old != null)
					log.warn("conflicting alias found: `{}` is hiding `{}` on alias `{}`", ctx.envelope(), old.envelope(), alias);
			}
		}
	}
	
	public Optional<ListenerReference> listener(String name) {
		return Optional.ofNullable(listeners.get(name));
	}
	
	public Map<String, ListenerReference> listeners() {
		return Collections.unmodifiableMap(listeners);
	}
	
	public Optional<ListenerReference> alias(String alias) {
		return Optional.ofNullable(aliases.get(alias));
	}
	
	@SuppressWarnings({"OptionalGetWithoutIsPresent", "ReturnOfInnerClass"}) // we want the exception and this is a short lived return object
	public ListenerReference listener(ChrislieListener listener) throws NoSuchElementException {
		return listeners.values().stream().filter(ctx -> ctx.envelope().listener() == listener).findAny().get();
	}
	
	/* While this class extends ListenerReference, it can enter states that are invalid for ListenerReference instances. However, the ChrislieContext implementation
	 * ensures that such instances are never actually leaked into the application. The reason the ListenerContext is extending the base class is to provide a unified
	 * way of accessing both isolated listener references that are part of a ChrislieGroup, as well as accessing aggregated listener references like they are
	 * provided by this class.
	 */
	@ToString
	private final class ListenerContext extends ListenerReference {
		
		private ListenerContext() {
			// these defaults are useless and will never leak into the application since the guaranteed pending apply() call will fix this state
			super(null, null, null, null, null);
		}
		
		/**
		 * Squashes the given ListenerReference into this one, overriding it if the given listener differs from the current one or merging it, if it doesn't.
		 *
		 * @param o Another ListenerContext that will override the current context.
		 */
		public void apply(ListenerReference o) {
			assert !(o instanceof ListenerContext);
			
			// if listener instance differs, we drop the existing context, this also happens on the first merge since listener is initialized with null
			if (envelope != o.envelope()) {
				name = o.name();
				help = o.help() != null ? o.help() : help; // only override if set
				
				envelope = o.envelope();
				
				// section requires copying, since we can't just take a reference, this would make us modify existing ListenerReferences once we call this method a second time
				// current context flexconfg will be used as a fallback in all listener references but since this flex conf can change multiple times, we use it as a fallback, rather then a snapshot
				super.flexConf = FlexConf.fallback(ChrislieContext.this.flexConf).apply(o.flexConf()); // copies o.flexConf but adds fallback to global context flexConf
				aliasSet = new AliasSet(o.aliasSet());
				return;
			}
			
			// otherwise we apply both the flex conf and alias set from the other listener context
			super.flexConf.apply(o.flexConf());
			aliasSet.apply(o.aliasSet());
		}
	}
}
