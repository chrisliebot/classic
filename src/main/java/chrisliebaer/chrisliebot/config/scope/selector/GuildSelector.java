package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import lombok.NonNull;

public class GuildSelector implements Selector {
	
	private final ChrislieGuild guild;
	
	public GuildSelector(@NonNull ChrislieGuild guild) {
		this.guild = guild;
	}
	
	@Override
	public boolean check(ChrislieMessage message) {
		return check(message.channel());
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return channel.guild().map(this::check).orElse(false);
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return false;
	}
	
	@Override
	public boolean check(ChrislieGuild guild) {
		return this.guild.identifier().equals(guild.identifier());
	}
}
