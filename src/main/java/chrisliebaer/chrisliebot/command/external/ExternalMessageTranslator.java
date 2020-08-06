package chrisliebaer.chrisliebot.command.external;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import com.google.gson.JsonElement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class translates Chrisliebots listener structures into various serializeable representations.
 */
@Slf4j
public final class ExternalMessageTranslator {
	
	private static final String ENV_PREFIX = "CB_";
	
	private Set<String> flexValues = new HashSet<>();
	
	public ExternalMessageTranslator withFlex(@NonNull String key) {
		flexValues.add(key);
		return this;
	}
	
	public ExternalInvocation toObject(@NonNull ChrislieListener.ListenerMessage m) {
		return ExternalInvocation.of(m, flexValues);
	}
	
	public ExternalInvocation toObject(@NonNull ChrislieListener.Invocation invc) {
		return ExternalInvocation.of(invc, flexValues);
	}
	
	public Map<String, String> toFlatMap(@NonNull ChrislieListener.ListenerMessage m) throws ChrislieListener.ListenerException {
		Map<String, Object> map = new HashMap<>();
		toFlatMap(map, flexValues, m);
		return stringifyMap(map);
	}
	
	public Map<String, String> toFlatMap(@NonNull ChrislieListener.Invocation m) throws ChrislieListener.ListenerException {
		Map<String, Object> map = new HashMap<>();
		toFlatMap(map, flexValues, m);
		return stringifyMap(map);
	}
	
	private static Map<String, String> stringifyMap(Map<String, Object> map) {
		var stringMap = new HashMap<String, String>(map.size());
		for (var e : map.entrySet()) {
			stringMap.put(ENV_PREFIX + e.getKey(), String.valueOf(e.getValue()));
		}
		return stringMap;
	}
	
	private static void toFlatMap(Map<String, Object> map, Set<String> flexValues, ChrislieListener.Invocation m) throws ChrislieListener.ListenerException {
		map.put("argument", m.arg());
		map.put("alias", m.alias());
		
		toFlatMap(map, flexValues, (ChrislieListener.ListenerMessage) m);
	}
	
	private static void toFlatMap(Map<String, Object> map, Set<String> flexValues, ChrislieListener.ListenerMessage m) throws ChrislieListener.ListenerException {
		var msg = m.msg();
		var channel = msg.channel();
		
		map.put("message", msg.message());
		
		map.put("user.identifier", msg.user().identifier());
		map.put("user.displayName", msg.user().displayName());
		map.put("user.mention", msg.user().mention());
		
		map.put("channel.identifier", channel.identifier());
		map.put("channel.displayName", channel.displayName());
		map.put("channel.isDirectMessage", channel.isDirectMessage());
		map.put("channel.isNSFW", channel.isNSFW());
		
		channel.guild().ifPresent(g -> {
			map.put("guild.identifier", g.identifier());
			map.put("guild.displayName", g.displayName());
		});
		
		var flex = m.ref().flexConf();
		for (String key : flexValues) {
			var v = flex.get(key, JsonElement.class);
			if (v.isPresent()) {
				var json = v.get();
				if (json.isJsonPrimitive()) {
					try {
						map.put("flex." + key, json.getAsString());
					} catch (ClassCastException | IllegalArgumentException e) {
						throw new ChrislieListener.ListenerException("unable to convert flex conf to string, value: " + json, e);
					}
				}
			}
		}
	}
	
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	public static class ExternalInvocation {
		
		private String argument; // non-null argument string
		private String alias; // alias that was used to invoke this command
		private String message; // full incoming message
		private Map<String, JsonElement> flex; // the flex config of this command, only explicitly included fields will be present
		
		private String service; // service identifier
		private ExternalUser user;
		private ExternalChannel channel;
		
		public static ExternalInvocation of(ChrislieListener.Invocation invc, Set<String> flexValues) {
			var r = of((ChrislieListener.ListenerMessage) invc, flexValues);
			r.argument = invc.arg();
			r.alias = invc.alias();
			return r;
		}
		
		public static ExternalInvocation of(ChrislieListener.ListenerMessage m, Set<String> flexValues) {
			var msg = m.msg();
			
			var r = new ExternalInvocation();
			r.service = m.service().identifier();
			r.user = ExternalUser.of(msg.user());
			r.channel = ExternalChannel.of(msg.channel());
			r.message = msg.message();
			
			var flex = m.ref().flexConf();
			r.flex = new HashMap<>(flexValues.size());
			for (String key : flexValues)
				r.flex.put(key, flex.get(key, JsonElement.class).orElse(null));
			
			return r;
		}
	}
	
	public static class ExternalUser {
		
		private String identifier;
		private String displayName;
		private String mention;
		
		public static ExternalUser of(ChrislieUser u) {
			var r = new ExternalUser();
			r.identifier = u.identifier();
			r.displayName = u.displayName();
			r.mention = u.mention();
			return r;
		}
	}
	
	public static class ExternalChannel {
		
		private String identifier;
		private String displayName;
		private boolean isDirectMessage;
		private List<ExternalUser> users;
		private boolean isNSFW;
		private ExternalGuild guild;
		
		public static ExternalChannel of(ChrislieChannel c) {
			var r = new ExternalChannel();
			r.identifier = c.identifier();
			r.displayName = c.displayName();
			r.isDirectMessage = c.isDirectMessage();
			// TODO: decide if we really need that
			//r.users = c.users().stream().map(ExternalUser::of).collect(Collectors.toList());
			r.isNSFW = c.isNSFW();
			r.guild = c.guild().map(ExternalGuild::of).orElse(null);
			return r;
		}
	}
	
	public static class ExternalGuild {
		
		private String identifier;
		private String displayName;
		
		public static ExternalGuild of(ChrislieGuild g) {
			var r = new ExternalGuild();
			r.identifier = g.identifier();
			r.displayName = g.displayName();
			return r;
		}
	}
}
