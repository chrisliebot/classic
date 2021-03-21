package chrisliebaer.chrisliebot.command.attachment;

import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class AttachmenViewerCommand implements ChrislieListener.Command {
	
	private Config cfg;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Generiert eine Vorschau für hochgeladene Textdateien aus der referenzierten antwort, so dass der Anhang nicht runtergeladen werden muss.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (!DiscordService.isDiscord(invc)) {
			ErrorOutputBuilder.generic("Dieses Feature steht nur auf Discord zu Verfügung.").write(invc).send();
			return;
		}
		
		var msg = (DiscordMessage) invc.msg();
		var discordMsg = msg.ev().getMessage();
		var ref = discordMsg.getReferencedMessage();
		
		if (ref == null) {
			// if no message is referenced, we provide viewer links for current message
			if (!discordMsg.getAttachments().isEmpty()) {
				ref = discordMsg;
			} else {
				ErrorOutputBuilder.generic("Du hast auf keine Nachricht geantwortet oder Discord hat gerade Probleme.").write(invc).send();
				return;
			}
		}
		
		var attachments = ref.getAttachments();
		if (attachments.isEmpty()) {
			ErrorOutputBuilder.generic("Die verlinkte Nachricht enthält keine Anhänge.").write(invc).send();
			return;
		}
		
		var out = invc.reply();
		out.title("Dateibetrachter");
		var desc = out.description();
		desc.append("[Zur Nachricht](" + ref.getJumpUrl() + ")").newLine().newLine();
		
		var joiner = desc.joiner("\n");
		for (var attachment : attachments) {
			var viewerLink = generateLink(ref, attachment);
			joiner.append("• [%s](%s)".formatted(MarkdownSanitizer.escape(attachment.getFileName()), viewerLink));
			joiner.seperator();
		}
		
		out.send();
	}
	
	@SneakyThrows // impossible, or else we crash ¯\_(ツ)_/¯
	private String generateLink(Message msg, Message.Attachment attachment) {
		var digest = MessageDigest.getInstance("SHA-256");
		
		var filename = attachment.getFileName();
		
		var hashInput = "%s/%s/%s%s".formatted(msg.getChannel().getId(), attachment.getId(), filename, cfg.hashSalt);
		var hash = digest.digest(hashInput.getBytes(StandardCharsets.UTF_8));
		var test = Arrays.copyOf(hash, cfg.hashLength);
		
		return "%s%s/%s/%s/%s".formatted(
				cfg.baseUrl,
				base64UrlEncode(long2byte(msg.getChannel().getIdLong())),
				base64UrlEncode(long2byte(attachment.getIdLong())),
				base64UrlEncode(filename.getBytes(StandardCharsets.UTF_8)),
				base64UrlEncode(Arrays.copyOf(hash, cfg.hashLength))
		);
	}
	
	private String base64UrlEncode(byte[] data) {
		var encoder = Base64.getEncoder();
		return encoder.encodeToString(data)
				.replace('+', '-')
				.replace('/', '_')
				.replace("=", "");
	}
	
	private byte[] long2byte(long l) {
		var b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putLong(l);
		return b.array();
	}
	
	private static class Config {
		
		private String baseUrl;
		private String hashSalt;
		private int hashLength;
	}
}
