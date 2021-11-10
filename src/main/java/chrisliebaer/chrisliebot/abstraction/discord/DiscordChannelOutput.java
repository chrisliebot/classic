package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class DiscordChannelOutput extends AbstractDiscordOutput<Message> {
	
	private final DiscordService service;
	private final DiscordMessage source;
	private final MessageChannel channel;
	
	private MessageAction messageAction;
	
	public DiscordChannelOutput(@NonNull DiscordService service, @NonNull MessageChannel channel) {
		this.service = service;
		this.channel = channel;
		source = null;
	}
	
	public DiscordChannelOutput(@NonNull DiscordService service, @NonNull MessageChannel channel, @NonNull DiscordMessage source) {
		this.service = service;
		this.channel = channel;
		this.source = source;
	}
	
	@Override
	protected CompletableFuture<Message> sink(SinkMessage message) {
		// TODO: properly catch and handle all sending errors via some way
		try {
			
			// assume we can upload (which is true for dm chats)
			boolean canUpload = true;
			
			// TODO: make this configurable via flex config
			if (channel instanceof TextChannel) {
				
				// check if we can upload files
				var textChannel = (TextChannel) channel;
				var selfMember = textChannel.getGuild().getMember(textChannel.getJDA().getSelfUser());
				canUpload = selfMember.hasPermission(textChannel, Permission.MESSAGE_ATTACH_FILES);
			}
			
			RestAction<Message> restAction = null;
			try {
				if (canUpload) {
					// inform user that we are processing request
					channel.sendTyping().queue();
					
					var sinkData = message.canUpload(service.bot().sharedResources().httpClient());
					var messageAction = channel.sendMessage(sinkData.message());
					
					for (var file : sinkData.files()) {
						messageAction = messageAction.addFile(file.download(), file.filename());
					}
					
					restAction = messageAction;
				}
			} catch (IOException e) {
				log.warn("failed to upload attachments, falling back to regular message", e);
				// regular upload follows after catch block and try has return statement
			}
			
			// there is sadly no way to know why restAction has not been populated yet, but if it isn't then we fall back to simple message
			if (restAction == null) {
				restAction = channel.sendMessage(message.noUpload());
			}
			
			// second special handling for text channels ¯\_(ツ)_/¯
			if (channel instanceof TextChannel) {
				var textChannel = (TextChannel) channel;
				if (textChannel.isNews()) {
					restAction = restAction.flatMap(Message::crosspost);
				}
			}
			
			var future = restAction.submit();
			return future.whenComplete((m, throwable) -> {
				if (throwable != null) {
					log.error("failed to send message", throwable);
					return;
				}
				
				// link send message with source message in database
				if (source != null) {
					source.service().traceMessage(source.ev().getMessage(), m);
				}
			});
		} catch (IllegalArgumentException e) { // if the message is too long or other undocumented shit inside jda
			log.error("failed to queue message", e);
			return CompletableFuture.failedFuture(e);
		}
	}
}
