package chrisliebaer.chrisliebot.command.cron;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import lombok.Getter;

import java.util.Optional;

public class CronCommandMessage implements ChrislieMessage {
	
	@Getter private final ChrislieUser user;
	@Getter private final ChrislieChannel channel;
	
	@Getter private final String message;
	private final ChrislieDispatcher.CommandParse commandParse;
	
	public CronCommandMessage(ChrislieUser user, ChrislieChannel channel, String message, ChrislieDispatcher.CommandParse commandParse) {
		this.user = user;
		this.channel = channel;
		this.message = message == null ? "" : message;
		this.commandParse = commandParse;
	}
	
	@Override
	public Optional<ChrislieDispatcher.CommandParse> forcedInvocation() {
		return Optional.of(commandParse);
	}
	
	@Override
	public ChrislieService service() {
		return channel.service();
	}
}
