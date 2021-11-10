package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutputImpl;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import okhttp3.OkHttpClient;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractDiscordOutput<RestObject> implements ChrislieOutput {
	
	
	private final EmbedBuilder embedBuilder = new EmbedBuilder();
	private final DiscordPlainOutput plain = new DiscordPlainOutput(AbstractDiscordOutput::escape4Discord, DiscordFormatter::format);
	private final PlainOutputImpl description = new PlainOutputImpl(AbstractDiscordOutput::escape4Discord, DiscordFormatter::format);
	
	private String authorName, authorUrl, authorIcon;
	
	
	/* jda will consider an embed with only a color to be valid, so setting the color based on the stack trace in the
	 * constructor will always create an embed, which is not what we want, so instead we track the color and only
	 * apply it during the final build operation if the color hasn't been set up until this point
	 *
	 * since we need the proper call stack for this, we have to store the color in the constructor
	 */
	private boolean colorSet = false;
	
	private URL imageUrl = null; // ideally, embed builder would allow us to just access the god damn thing
	
	private final Optional<Color> stackTraceColor = colorFromCallstack();
	
	@Override
	public AbstractDiscordOutput title(String title, String url) {
		embedBuilder.setTitle(title, url);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput image(String url) {
		try {
			imageUrl = new URL(url);
			embedBuilder.setImage(url);
		} catch (MalformedURLException e) {
			log.warn("malformed url for embed image", e);
		}
		return this;
	}
	
	@Override
	public AbstractDiscordOutput thumbnail(String url) {
		embedBuilder.setThumbnail(url);
		return this;
	}
	
	@Override
	public @NonNull PlainOutput description() {
		return description;
	}
	
	@Override
	public AbstractDiscordOutput color(Color color) {
		colorSet = true;
		embedBuilder.setColor(color);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput color(int color) {
		colorSet = true;
		embedBuilder.setColor(color);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput author(String name) {
		authorName = name;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput authorUrl(String url) {
		authorUrl = url;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput authorIcon(String url) {
		authorIcon = url;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput field(String field, String value, boolean inline) {
		embedBuilder.addField(field, value, inline);
		return this;
	}
	
	@Override
	public AbstractDiscordOutput footer(String text, String iconUrl) {
		embedBuilder.setFooter(text, iconUrl);
		return this;
	}
	
	@Override
	public PlainOutput plain() {
		return plain;
	}
	
	@Override
	public PlainOutput.PlainOutputSubstituion convert() {
		return PlainOutput.dummy();
	}
	
	@Override
	public PlainOutput replace() {
		return PlainOutput.dummy();
	}
	
	@Override
	public void send() {
		discordSend();
	}
	
	public CompletableFuture<RestObject> discordSend() {
		
		return sink(new SinkMessage());
	}
	
	protected abstract CompletableFuture<RestObject> sink(SinkMessage message);
	
	private static String escape4Discord(String s) {
		return MarkdownSanitizer.escape(s);
	}
	
	// highly illegal method of creating command dependant colors (please don't tell anyone)
	private static Optional<Color> colorFromCallstack() {
		var st = Thread.currentThread().getStackTrace();
		
		// now we walk up the stacktrace until we find something that implements ChrislieListener interface
		try {
			for (var e : st) {
				var clazz = Class.forName(e.getClassName());
				while (clazz != null) {
					
					// ChrislieListener is of part of invocation, so we need to exclude it
					if (clazz != ChrislieListener.class && ChrislieListener.class.isAssignableFrom(clazz))
						return Optional.of(C.hashColor(clazz.getSimpleName().getBytes(StandardCharsets.UTF_8)));
					
					// walk up to outer class
					clazz = clazz.getEnclosingClass();
				}
			}
		} catch (ClassNotFoundException e) {
			log.warn("you won't believe it but we can't even find the class that called us", e);
		}
		return Optional.empty();
	}
	
	/**
	 * This object encapsulates a message generated by the message abstraction framework. It is up to the receiver to decide wether supporting uplods is possible. File
	 * uploads are done lazy and <i>pulled</i> from the output object.
	 */
	public class SinkMessage {
		
		private Message prepare(EmbedBuilder localEmbedBuilder) {
			localEmbedBuilder.setDescription(description.string());
			MessageBuilder mb = new MessageBuilder();
			
			// block all mentions by default and apply collected mention rules from output instance
			mb.setAllowedMentions(List.of());
			plain.applyMentionRules(mb);
			
			if (!localEmbedBuilder.isEmpty()) {
				
				// jda considers embed non-empty if color has been set
				if (!colorSet)
					stackTraceColor.ifPresent(localEmbedBuilder::setColor);
				
				mb.setEmbed(localEmbedBuilder.build());
			}
			
			mb.append(plain.string());
			return mb.build();
		}
		
		/**
		 * Indicates that the receiving sink is not capable of performing file uploads.
		 *
		 * @return The requested message.
		 */
		public Message noUpload() {
			return prepare(embedBuilder);
		}
		
		/**
		 * Indicates that the receiving sink is capable of performing file uplods.
		 *
		 * @param okHttpClient Client that is used for downloading attached files.
		 * @return Object containing message and files that need to be uploaded together with message.
		 */
		public SinkMessageData canUpload(OkHttpClient okHttpClient) {
			// copy embed builder since we are potentially changing image
			var localEmbedBuilder = new EmbedBuilder(embedBuilder);
			
			var files = new ArrayList<SinkMessageData.UploadFile>();
			if (imageUrl != null) {
				// check if we can derive a file name for this url
				var filePath = Path.of(imageUrl.getPath());
				var file = filePath.getFileName();
				if (file != null) {
					var filename = file.toString();
					// TODO: when supporting multiple files, generate random file names to avoid clashes
					files.add(new SinkMessageData.UploadFile(okHttpClient, filename, imageUrl));
					
					localEmbedBuilder.setImage("attachment://" + filename); // update embed file name
				}
			}
			
			// construct message with modified embed builder
			var message = prepare(localEmbedBuilder);
			
			return new SinkMessageData(message, files);
		}
	}
	
	@Data
	@AllArgsConstructor
	public static class SinkMessageData {
		
		private final Message message;
		private final List<UploadFile> files;
		
		@Data
		@AllArgsConstructor
		public static class UploadFile {
			
			private final OkHttpClient client;
			
			private String filename;
			private URL url;
			
			/**
			 * Downloads the backing file.
			 *
			 * @return The data of the downloaded file.
			 * @throws IOException If an error occurs during download.
			 */
			public byte[] download() throws IOException {
				return C.downloadFile(client, url);
			}
		}
	}
}
