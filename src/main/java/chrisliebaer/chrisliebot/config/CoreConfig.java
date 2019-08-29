package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.abstraction.discord.DiscordBootstrap;
import chrisliebaer.chrisliebot.abstraction.irc.IrcBootstrap;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToString
public class CoreConfig {
	
	@Getter private String databasePool;
	
	@Getter private Map<String, IrcBootstrap> irc = Map.of();
	@Getter private Map<String, DiscordBootstrap> discord = Map.of();
	
	public void ensureDisjoint() {
		Set<String> set = new HashSet<>();
		ensureDisjoint(set, irc.keySet());
		ensureDisjoint(set, discord.keySet());
	}
	
	private static void ensureDisjoint(Set<String> set, Set<String> serviceKeys) {
		for (String s : serviceKeys) {
			if (set.contains(s))
				throw new IllegalArgumentException(String.format("duplicated service with name `%s`", s));
			set.add(s);
		}
	}
}
