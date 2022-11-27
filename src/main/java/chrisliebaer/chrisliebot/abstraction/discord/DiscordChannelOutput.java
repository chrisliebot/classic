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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	protected CompletableFuture<Message> send(SinkMessage message) {
		return flush(message, new SendSink());
	}
	
	@Override
	protected CompletableFuture<Message> edit(SinkMessage message, long messageId) {
		return flush(message, new EditSink(messageId));
	}
	
	private CompletableFuture<Message> flush(SinkMessage message, Sink sink) {
		// TODO: properly catch and handle all sending errors via some way
		try {
			
			RestAction<Message> restAction = null;
			try {
				if (sink.canUpload()) {
					// inform user that we are processing request
					try {
						channel.sendTyping().submit().get(200, TimeUnit.MILLISECONDS); // attempt to wait for short period to reduce bugged typing states
					} catch (InterruptedException | ExecutionException | TimeoutException ignore) {
					}
					
					var sinkData = message.canUpload(service.bot().sharedResources().httpClient());
					var messageAction = sink.create(sinkData.message());
					
					for (var file : sinkData.files()) {
						messageAction = messageAction.addFile(file.download(), file.filename());
					}
					
					restAction = messageAction;
				}
			} catch (IOException e) {
				log.warn("failed to upload attachments, falling back to regular message", e);
				// regular upload follows after catch block if restAction is still null
			}
			
			// there is sadly no way to know why restAction has not been populated yet, but if it isn't then we fall back to simple message
			if (restAction == null) {
				restAction = sink.create(message.noUpload());
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
	
	private interface Sink {
		
		boolean canUpload();
		MessageAction create(Message message);
	}
	
	private final class SendSink implements Sink {
		
		@Override
		public boolean canUpload() {
			// assume we can upload (which is true for dm chats)
			boolean canUpload = true;
			
			// TODO: make this configurable via flex config
			if (channel instanceof TextChannel) {
				
				// check if we can upload files
				var textChannel = (TextChannel) channel;
				var selfMember = textChannel.getGuild().getMember(textChannel.getJDA().getSelfUser());
				canUpload = selfMember.hasPermission(textChannel, Permission.MESSAGE_ATTACH_FILES);
			}
			return canUpload;
		}
		
		@Override
		public MessageAction create(Message message) {
			return channel.sendMessage(message);
		}
	}
	
	private final class EditSink implements Sink {
		
		private EditSink(long messageId) {
			this.messageId = messageId;
		}
		
		private final long messageId;
		
		@Override
		public boolean canUpload() {
			// discord prevents editing of messages with new attachments
			return false;
		}
		
		@Override
		public MessageAction create(Message message) {
			return channel.editMessageById(messageId, message);
		}
	}
}
