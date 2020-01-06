package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

import javax.validation.constraints.NotBlank;
import java.util.Objects;

public class UserIsPartOfGuild implements Selector {
	
	private Config cfg;
	
	@Override
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		cfg = Objects.requireNonNull(gson.fromJson(json, Config.class), "config is null");
	}
	
	@Override
	public boolean check(ChrislieMessage message) {
		var isOtherGuild = message.channel().guild()
				.map(g -> !g.identifier().equals(cfg.guild)) // true if mismatch => other guild
				.orElse(false);
		
		// other guilds requires opt int
		if (isOtherGuild && !cfg.includeOthers)
			return false;
		
		return check(message.user());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		var guild = user.service().guild(cfg.guild);
		return guild.map(g -> g.users().contains(user)).orElse(false);
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		
		// listeners that are accesing private channels might not know they are targeting a single user and thous only use the channel handle
		if (channel.isDirectMessage())
			return channel.users().stream().anyMatch(this::check);
		return false;
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
	
	private static class Config {
		
		private @NotBlank String guild;
		private boolean includeOthers; // if true, will match even if inside other guild
	}
}
