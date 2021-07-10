package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RegExpSelector implements Selector {
	
	private List<Function<DataContainer, Boolean>> fns;
	
	@Override
	public boolean check(ChrislieMessage message) {
		return test(DataContainer.builder()
				.service(message.service().identifier())
				.guild(message.channel().guild().map(ChrislieGuild::identifier).orElse(null))
				.channel(message.channel().identifier())
				.user(message.user().identifier())
				.displayName(message.user().displayName())
				.message(message.message())
				
				.build());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return test(DataContainer.builder()
				.service(user.service().identifier())
				.user(user.identifier())
				.displayName(user.displayName())
				
				.build());
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return test(DataContainer.builder()
				.service(channel.service().identifier())
				.guild(channel.guild().map(ChrislieGuild::identifier).orElse(null))
				.channel(channel.identifier())
				
				.build());
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return test(DataContainer.builder()
				.service(service.identifier())
				
				.build());
	}
	
	@Override
	public boolean check(ChrislieGuild guild) {
		return test(DataContainer.builder()
				.service(guild.service().identifier())
				.guild(guild.identifier())
				
				.build());
	}
	
	private boolean test(DataContainer c) {
		return fns.stream().allMatch(f -> f.apply(c));
	}
	
	@SuppressWarnings("EmptyClass")
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		var type = new TypeToken<Map<String, String>>() {}.getType();
		Map<String, String> map = gson.fromJson(json, type);
		
		fns = new ArrayList<>(map.size());
		
		for (var e : map.entrySet()) {
			var key = e.getKey();
			var pattern = e.getValue();
			var accessor = DataContainer.accessor(key);
			if (accessor.isEmpty())
				throw new SelectorException(String.format("unknown key `%s` could not be mapped to accessor", key));
			
			if (pattern == null)
				throw new SelectorException(String.format("pattern for key `%s` is null", key));
			
			// if pattern starts with r: it is parsed as a regex
			Predicate<String> predicate;
			if (pattern.startsWith("r:"))
				predicate = Pattern.compile(pattern.substring(2)).asPredicate();
			else
				predicate = pattern::equals;
			
			// build function that takes container, uses accessor to extract value and tests with the given predicate
			fns.add(c -> {
				var val = accessor.get().apply(c);
				return val != null && predicate.test(val);
			});
		}
	}
	
	@Builder
	private static class DataContainer {
		
		@Getter private final String service;
		@Getter private final String guild;
		@Getter private final String channel;
		@Getter private final String user;
		@Getter private final String displayName;
		@Getter private final String message;
		
		public static Optional<Function<DataContainer, String>> accessor(String key) {
			if (key == null)
				return Optional.empty();
			
			return switch (key) {
				case "service" -> Optional.of(DataContainer::service);
				case "guild" -> Optional.of(DataContainer::guild);
				case "channel" -> Optional.of(DataContainer::channel);
				case "user" -> Optional.of(DataContainer::user);
				case "displayName" -> Optional.of(DataContainer::displayName);
				case "message" -> Optional.of(DataContainer::message);
				default -> Optional.empty();
			};
		}
	}
}
