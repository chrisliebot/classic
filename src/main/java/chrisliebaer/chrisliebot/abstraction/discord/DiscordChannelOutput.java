package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class DiscordChannelOutput extends AbstractDiscordOutput<Message> {
	
	private final DiscordMessage source;
	private final MessageChannel channel;
	
	public DiscordChannelOutput(@NonNull MessageChannel channel) {
		this.channel = channel;
		source = null;
	}
	
	public DiscordChannelOutput(@NonNull MessageChannel channel, @NonNull DiscordMessage source) {
		this.channel = channel;
		this.source = source;
	}
	
	@Override
	protected CompletableFuture<Message> sink(Message m) {
		// TODO: properly catch and handle all sending errors via some way
		try {
			RestAction<Message> restAction = channel.sendMessage(m);
			
			// TODO: make this configurable via flex config
			if (channel instanceof TextChannel) {
				var textChannel = (TextChannel) channel;
				if (textChannel.isNews()) {
					restAction = restAction.flatMap(Message::crosspost);
				}
			}
			
			var future = restAction.submit();
			return future.whenComplete((message, throwable) -> {
				if (throwable != null) {
					log.error("failed to send message", throwable);
					return;
				}
				
				// link send message with source message in database
				if (source != null) {
					source.service().traceMessage(source.ev().getMessage(), message);
				}
			});
		} catch (IllegalArgumentException e) { // if the message is too long or other undocumented shit inside jda
			log.error("failed to queue message", e);
			return CompletableFuture.failedFuture(e);
		}
	}
}
