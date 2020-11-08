package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import chrisliebaer.chrisliebot.config.scope.ScopeMapping;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.config.scope.selector.AcceptAllSelector;
import chrisliebaer.chrisliebot.config.scope.selector.CombinationSelector;
import chrisliebaer.chrisliebot.config.scope.selector.DiscordPermissionSelector;
import chrisliebaer.chrisliebot.config.scope.selector.IrcChannelFlagSelector;
import chrisliebaer.chrisliebot.config.scope.selector.NSFWSelector;
import chrisliebaer.chrisliebot.config.scope.selector.RegExpSelector;
import chrisliebaer.chrisliebot.config.scope.selector.ServiceIdentifierSelector;
import chrisliebaer.chrisliebot.config.scope.selector.ServiceSelector;
import chrisliebaer.chrisliebot.config.scope.selector.UserExistsInChannel;
import chrisliebaer.chrisliebot.config.scope.selector.UserIsPartOfGuild;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

@Slf4j
@ToString
public class JsonBotConfig { // TODO: replace code validation with java bean validation
	
	private List<ScopeToGroup> mappings; // contains scope selectors and groups
	private Map<String, Group> groups; // list of groups with their command anchors
	
	@ToString
	private static class ScopeToGroup {
		
		private List<JsonSelector> selectors;
		private Set<String> groups;
	}
	
	@ToString
	private static class JsonSelector {
		
		private String type;
		private JsonElement json;
	}
	
	@ToString
	private static class Group {
		
		private List<String> include; // name of groups that will be included in this group, in the implementation, these groups are processed before this one
		private Map<String, JsonElement> flex; // separate flex conf that attaches values to a group but also acts as a fallback for flexconf of listeners
		private List<ListenerAnchor> listener; // listener definition
	}
	
	public List<ScopeMapping> instance(GsonValidator gson) throws ConfigInitializeException {
		return new Builder().instanceScopeMapping(gson);
	}
	
	/**
	 * Defines commands.
	 */
	@ToString
	private static class ListenerAnchor {
		// both ref and def are defined within the same class so we can ensure the uer didn't provide conflicting fields
		
		private String type; // ref, def
		
		private String name; // identifier
		private String help; // help text, valid in ref and def
		
		private String clazz; // name of implementing listener, only if def
		
		private JsonElement cfg; // static config, only valid if def
		private Map<String, JsonElement> flex; // flex conf, configures behavior and can be redefined, valid in def and ref
		
		/**
		 * Only valid for listeners with command handler. Defines set of strings that are mapped into command space. Set
		 * to empty set to a set with no mappings. Keep null
		 * <p>
		 * Prefixing with - or + will modify the existing alias map from parent scopes. Otherwise, any existing map will
		 * be overriden. An alias with the suffix ? will be hidden and not publically exposed. This is usefull if you
		 * want to include misspelled versions of the command.
		 */
		private Set<String> alias;
		
		@SuppressWarnings("VariableNotUsedInsideIf") // validation is for user, not for program
		public ListenerReference instance(GsonValidator gson, ChrislieContext ctx, String groupName) throws ChrislieListener.ListenerException {
			
			// check for common fields
			if (name == null || name.isBlank())
				throw new ChrislieListener.ListenerException("no name given");
			if (type == null)
				throw new ChrislieListener.ListenerException("no type given");
			
			// validate type and create or fetch listener from context
			ChrislieListener.Envelope envelope;
			if ("def".equals(type)) {
				if (clazz == null || clazz.isBlank())
					throw new ChrislieListener.ListenerException("no clazz specified");
				
				envelope = instanceListener(gson, groupName);
			} else if ("ref".equals(type)) {
				if (clazz != null)
					throw new ChrislieListener.ListenerException("clazz is only valid in listener definition");
				if (cfg != null)
					throw new ChrislieListener.ListenerException("cfg is only valid in listener definition");
				
				// look up listener in provided context or fail if there is no such listener
				envelope = ctx.listener(name)
						.orElseThrow(() -> new ChrislieListener.ListenerException("failed to locate referenced listener in current context"))
						.envelope();
			} else
				throw new ChrislieListener.ListenerException("invalid or empty anchor type");
			
			// create alias set if listener implements command
			AliasSet aliasSet;
			if (alias != null) {
				if (envelope.listener() instanceof ChrislieListener.Command)
					aliasSet = AliasSet.of(alias).orElseThrow(() -> new ChrislieListener.ListenerException("invalid alias map: " + alias));
				else
					throw new ChrislieListener.ListenerException("anchor has alias map but listener doesn't implement command interface");
			} else {
				// giving every reference an empty alias set makes the context builder logic simpler, since there won't be any null references
				aliasSet = new AliasSet();
			}
			
			// always returns a listener reference since it doesn't matter where they are from, this results in references being the only way to reference a listener
			return ListenerReference.builder()
					.name(name)
					.help(help)
					.envelope(envelope)
					.flexConf(instanceFlexConf(gson, flex))
					.aliasSet(aliasSet)
					.build();
		}
		
		private ChrislieListener.Envelope instanceListener(GsonValidator gson, String groupName) throws ChrislieListener.ListenerException {
			try {
				Class<? extends ChrislieListener> clazz = Class.forName(this.clazz).asSubclass(ChrislieListener.class);
				ChrislieListener listener = clazz.getDeclaredConstructor().newInstance();
				listener.fromConfig(gson, cfg);
				return new ChrislieListener.Envelope(listener, format("created as listener `%s` in group `%s`", name, groupName));
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
				throw new ChrislieListener.ListenerException("failed to instance listener", e);
			}
		}
	}
	
	private static FlexConf instanceFlexConf(GsonValidator gson, Map<String, JsonElement> flex) {
		flex = flex == null ? Map.of() : flex;
		return JsonFlexConfResolver.of(gson, Collections.unmodifiableMap(flex));
	}
	
	/**
	 * Builder manages temporary variables during instance creation and contains instance logic.
	 */
	private class Builder {
		
		private GsonValidator gson;
		private Map<String, ChrislieGroup> chrislieGroups;
		
		public List<ScopeMapping> instanceScopeMapping(GsonValidator gson) throws ConfigInitializeException {
			this.gson = gson;
			
			chrislieGroups = new HashMap<>();
			
			ensureNonNull(groups, "group list is null");
			ensureNonNull(mappings, "mapping list is null");
			
			// iterate over all mappings (note that a mapping may consist of multiple selectors)
			List<ScopeMapping> scopeMappings = new ArrayList<>(mappings.size());
			for (ScopeToGroup mapping : mappings) {
				
				ensureNonNull(mapping, "mapping itself is null");
				ensureNonNull(mapping.groups, "group list in mapping is null");
				ensureNonNull(mapping.selectors, "selector list in mapping is null");
				
				// try to resolve group names first, since it is faster and better to fail at this stage rather than executing selector code before failing
				List<ChrislieGroup> mappingGroups = new ArrayList<>(mapping.groups.size());
				for (String name : mapping.groups) {
					
					// instancing group only when referenced by mapping allows checking for orphaned groups after loop
					var group = groups.get(name);
					ensureNonNull(group, format("there is no group with name `%s` referenced by mapping: %s", name, mapping));
					
					// will return existing group when called twice with same group
					var g = instanceGroup(name, group, new ArrayList<>());
					mappingGroups.add(g);
				}
				
				List<Selector> selectors = new ArrayList<>(mapping.selectors.size());
				for (JsonSelector selector : mapping.selectors) {
					try {
						selectors.add(instanceSelector(selector));
					} catch (Selector.SelectorException e) {
						throw new ConfigInitializeException(format("failed to instance selector: `%s` in mapping %s", selector, mapping), e);
					}
				}
				
				scopeMappings.add(new ScopeMapping(selectors, mappingGroups));
			}
			
			// compare list of instanced groups with list of all existing groups in config and report difference as orphans
			groups.keySet().forEach(chrislieGroups::remove);
			
			if (!chrislieGroups.isEmpty())
				log.warn("found orphaned groups that are never used: {}", chrislieGroups.keySet());
			
			return scopeMappings;
		}
		
		@SuppressWarnings("EmptyClass")
		private List<Selector> instanceSelectors(@NonNull JsonElement json) throws Selector.SelectorException {
			List<JsonSelector> jsonSelectors = gson.fromJson(json, new TypeToken<List<JsonSelector>>() {}.getType());
			List<Selector> selectors = new ArrayList<>(jsonSelectors.size());
			for (JsonSelector selector : jsonSelectors)
				selectors.add(instanceSelector(selector));
			return selectors;
		}
		
		private Selector instanceSelector(JsonSelector json) throws Selector.SelectorException {
			Selector selector = switch (json.type) {
				case "all" -> new AcceptAllSelector();
				case "or" -> CombinationSelector.or(instanceSelectors(json.json));
				case "and" -> CombinationSelector.and(instanceSelectors(json.json));
				case "nsfw" -> new NSFWSelector();
				case "regex" -> new RegExpSelector();
				case "userExistsInChannel" -> new UserExistsInChannel();
				case "userIsPartOfGuild" -> new UserIsPartOfGuild();
				case "service" -> new ServiceIdentifierSelector();
				case "irc" -> new ServiceSelector.IrcSelector();
				case "discord" -> new ServiceSelector.DiscordSelector();
				case "ircChannelFlag" -> new IrcChannelFlagSelector();
				case "discordPermission" -> new DiscordPermissionSelector();
				default -> throw new Selector.SelectorException(format("there is no selector of type `%s`", json.type));
			};
			selector.fromJson(gson, json.json);
			return selector;
		}
		
		private ChrislieGroup instanceGroup(String name, Group group, List<String> includeList) throws ConfigInitializeException {
			// check for inheritance cycle
			if (includeList.contains(name))
				throw new ConfigInitializeException("group inheritance cycle found: " + includeList);
			
			log.trace("instancing group {}: {}", name, group);
			
			try {
				includeList.add(name);
				
				// resolve includes
				ChrislieGroup g = chrislieGroups.get(name);
				if (g == null) {
					
					if (group.listener == null)
						group.listener = List.of();
					
					// create group (and parent groups)
					List<ChrislieGroup> list = new ArrayList<>();
					
					// skip groups with no includes
					if (group.include != null) {
						for (String include : group.include)
							list.add(instanceGroup(include, JsonBotConfig.this.groups.get(include), includeList));
					}
					
					// build context from include list
					ChrislieContext ctx = new ChrislieContext(list);
					
					// resolve listener and create reference in group
					Set<String> nameTracker = new HashSet<>(group.listener.size()); // prevent duplicated listener names
					List<ListenerReference> refs = new ArrayList<>(group.listener.size());
					for (ListenerAnchor anchor : group.listener) {
						if (nameTracker.contains(anchor.name))
							throw new ConfigInitializeException(format("duplicated listener name in group `%s`, conflicting anchor was: %s", name, anchor));
						
						try {
							refs.add(anchor.instance(gson, ctx, name));
						} catch (ChrislieListener.ListenerException e) {
							throw new ConfigInitializeException(format("failed to instance listener anchor in group `%s`, anchor was: %s", name, anchor), e);
						}
						
						nameTracker.add(anchor.name);
					}
					
					g = new ChrislieGroup(name, instanceFlexConf(gson, group.flex), list, refs);
					
					// add group to instanced groups as this group is now fully functional
					chrislieGroups.put(name, g);
				}
				
				return g;
			} finally {
				// there can only ever be exactly one kind of the same string, so we know for sure we remove the correct element
				includeList.remove(name);
			}
		}
		
		private <T> T ensureNonNull(T o, String msg) throws ConfigInitializeException {
			if (o == null)
				throw new ConfigInitializeException(msg);
			return o;
		}
	}
	
	@Slf4j
	@ToString
	@SuppressWarnings("DuplicatedCode")
	private static final class JsonFlexConfResolver implements FlexConf.Resolver {
		
		private GsonValidator gson;
		private Map<String, JsonElement> map;
		
		public static FlexConf of(GsonValidator gson, Map<String, JsonElement> map) {
			return new FlexConf(new JsonFlexConfResolver(gson, map));
		}
		
		private JsonFlexConfResolver(GsonValidator gson, Map<String, JsonElement> map) {
			this.gson = gson;
			this.map = map;
		}
		
		@Override
		public <V> Optional<V> get(String key, Class<V> clazz) {
			var val = map.get(key);
			if (val == null)
				return Optional.empty();
			
			try {
				return Optional.ofNullable(gson.fromJson(val, clazz));
			} catch (JsonSyntaxException e) {
				log.warn("failed to get key `{}` from flexconf as `{}` with json representation `{}` flexconf is: {}", key, clazz, val, this, e);
				return Optional.empty();
			}
		}
		
		@Override
		public Optional<Object> get(String key, Type type) {
			var val = map.get(key);
			if (val == null)
				return Optional.empty();
			
			try {
				return Optional.ofNullable(gson.fromJson(val, type));
			} catch (JsonSyntaxException e) {
				log.warn("failed to get key `{}` from flexconf as `{}` with json representation `{}` flexconf is: {}", key, type, val, this, e);
				return Optional.empty();
			}
		}
	}
	
	
	/**
	 * Indicates failure to load the provided configuration. This does not indicate a dirty state, since all instance
	 * objects are not permitted to perform startup action up until this point.
	 */
	public static class ConfigInitializeException extends Exception {
		
		public ConfigInitializeException() {
		}
		
		public ConfigInitializeException(String message) {
			super(message);
		}
		
		public ConfigInitializeException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ConfigInitializeException(Throwable cause) {
			super(cause);
		}
	}
}
