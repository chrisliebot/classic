package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordSlashCommandMessage implements ChrislieMessage {
	
	@Getter private final DiscordService service;
	@Getter private final SlashCommandEvent ev;
	
	@Getter private final DiscordChannel channel;
	
	private final ChrislieDispatcher.CommandParse parse;
	
	public DiscordSlashCommandMessage(@NonNull DiscordService service, @NonNull SlashCommandEvent ev) {
		this.service = service;
		this.ev = ev;
		
		switch (ev.getChannelType()) {
			case TEXT -> channel = new DiscordGuildChannel(service, ev.getTextChannel());
			case PRIVATE -> channel = new DiscordPrivateChannel(service, ev.getPrivateChannel());
			default -> throw new RuntimeException("message was sent in unkown channel type");
		}
		
		var arg = ev.getOption(DiscordService.SLASH_COMMAND_ARG_NAME);
		parse = new ChrislieDispatcher.CommandParse(ev.getName(), arg == null ? "" : arg.getAsString());
	}
	
	@Override
	public Optional<ChrislieDispatcher.CommandParse> forcedInvocation() {
		return Optional.of(parse);
	}
	
	@Override
	public DiscordUser user() {
		return new DiscordUser(service, ev.getUser());
	}
	
	@Override
	public String message() {
		return parse.args();
	}
	
	@Override
	public ChrislieOutput reply(LimiterConfig limiter) {
		return new AbstractDiscordOutput<>() {
			private boolean isError;
			
			@Override
			public void markAsError() {
				isError = true;
			}
			
			@Override
			protected CompletableFuture sink(Message message) {
				if (ev.isAcknowledged()) {
					return ev.getHook().sendMessage(message).setEphemeral(isError).submit();
				} else {
					return ev.reply(message).setEphemeral(isError).submit();
				}
			}
		};
	}
}
