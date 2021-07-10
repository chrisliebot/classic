package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.config.scope.Selector;

public abstract class ServiceSelector implements Selector {
	
	@Override
	public boolean check(ChrislieMessage message) {
		return check(message.service());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return check(user.service());
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return check(channel.service());
	}
	
	@Override
	public boolean check(ChrislieGuild guild) {
		return check(guild.service());
	}
	
	public static class DiscordSelector extends ServiceSelector {
		
		@Override
		public boolean check(ChrislieService service) {
			return service instanceof DiscordService;
		}
	}
	
	public static class IrcSelector extends ServiceSelector {
		
		@Override
		public boolean check(ChrislieService service) {
			return service instanceof IrcService;
		}
	}
}
