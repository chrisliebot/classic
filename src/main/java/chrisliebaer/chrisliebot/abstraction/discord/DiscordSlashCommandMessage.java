package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class DiscordSlashCommandMessage implements ChrislieMessage {
	
	@Getter private final DiscordService service;
	@Getter private final SlashCommandInteractionEvent ev;
	
	@Getter private final DiscordChannel channel;
	
	private final ChrislieDispatcher.CommandParse parse;
	
	public DiscordSlashCommandMessage(@NonNull DiscordService service, @NonNull SlashCommandInteractionEvent ev) {
		this.service = service;
		this.ev = ev;
		
		channel = DiscordChannel.of(service, ev.getChannel());
		
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
		return new AbstractDiscordOutput<Message>() {
			@Override
			protected CompletableFuture<Message> send(AbstractDiscordOutput<Message>.SinkMessage message) {
				// file uploads my cause delays, so we have to ack message in all cases
				if (!ev.isAcknowledged()) {
					ev.deferReply(isError).queue();
				}
				var hook = ev.getHook();
				
				// now we can pull files for upload (slash commands are always allowed to upload files)
				var sinkData = message.canUpload(service.bot().sharedResources().httpClient());
				var restAction = hook.sendMessage(sinkData.message()).setEphemeral(isError);
				try {
					for (var file : sinkData.files()) {
						restAction = restAction.addFiles(FileUpload.fromData(file.download(), file.filename()));
					}
				} catch (IOException e) {
					log.warn("failed to upload attachments, falling back to regular message", e);
					restAction = hook.sendMessage(message.noUpload()).setEphemeral(isError);
				}
				
				return restAction.submit();
			}
			
			// the generic types are so broken
			private boolean isError;
			
			@Override
			public void markAsError() {
				isError = true;
			}
		};
	}
}
