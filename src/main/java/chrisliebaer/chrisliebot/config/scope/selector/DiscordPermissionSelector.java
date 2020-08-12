package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.config.scope.Selector;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.Permission;

import java.util.Set;

public class DiscordPermissionSelector implements Selector {
	
	private Set<Permission> permissions;
	
	@Override
	@SuppressWarnings("EmptyClass")
	public void fromJson(GsonValidator gson, JsonElement json) throws SelectorException {
		permissions = gson.fromJson(json, new TypeToken<Set<Permission>>(){}.getType());
	}
	
	@Override
	public boolean check(ChrislieMessage chrislieMessage) {
		if (chrislieMessage instanceof DiscordMessage message) {
			var maybeGuild = message.channel().guild();
			if (maybeGuild.isEmpty())
				return false;
			var guild = maybeGuild.get().guild();
			var member = guild.getMember(message.user().user());
			if (member == null)
				return false;
			
			for (var perm : permissions)
				if (!member.hasPermission(perm))
					return false;
			
			return true;
		}
		
		return check(chrislieMessage.user());
	}
	
	@Override
	public boolean check(ChrislieUser chrislieUser) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
}
