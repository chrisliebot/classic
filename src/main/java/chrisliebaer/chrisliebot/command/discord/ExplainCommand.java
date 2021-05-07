package chrisliebaer.chrisliebot.command.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.config.flex.CommonFlex;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.util.Optional;
import java.util.regex.Pattern;

public class ExplainCommand implements ChrislieListener.Command {
	
	// the one that comes with JDA can't handle DM refs
	private static final Pattern JUMP_URL_PATTERN = Pattern.compile(
			"(?:https?://)?" +                                             // Scheme
					"(?:\\w+\\.)?" +                                               // Subdomain
					"discord(?:app)?\\.com" +                                      // Discord domain
					"/channels/(?<guild>\\d+|@me)/(?<channel>\\d+)/(?<message>\\d+)" + // Path
					"(?:\\?\\S*)?(?:#\\S*)?",                                      // Useless query or URN appendix
			Pattern.CASE_INSENSITIVE);
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Antworte entweder mit diesem Befehl auf meine Nachricht oder geb mir einen Link zu einer anderen Nachricht von mir und ich sag dir, " +
				"warum ich das getan hab.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (!DiscordService.isDiscord(invc)) {
			ErrorOutputBuilder.generic("Dieser Befehlt steht nur auf Discord zur Verfügung.").write(invc).send();
			return;
		}
		
		var discordMessage = (DiscordMessage) invc.msg();
		var service = discordMessage.service();
		var jda = service.jda();
		var msg = discordMessage.ev().getMessage();
		var requestee = msg.getAuthor();
		
		// check of message jump url
		var ownMessage = msg.getReferencedMessage();
		var matcher = JUMP_URL_PATTERN.matcher(invc.arg().trim());
		if (matcher.matches()) {
			var guildId = matcher.group(1);
			long channelId = Long.parseLong(matcher.group(2));
			long messageId = Long.parseLong(matcher.group(3));
			
			try {
				if ("@me".equals(guildId)) {
					var channel = jda.getPrivateChannelById(channelId);
					if (channel != null)
						ownMessage = channel.retrieveMessageById(messageId).complete();
				} else {
					var channel = jda.getTextChannelById(channelId);
					if (channel != null)
						ownMessage = channel.retrieveMessageById(messageId).complete();
				}
			} catch (ErrorResponseException ignore) {
				// we will simply ignore these exception since failing to resolve message is not going to change the output, since we are not leaking your access rights
			}
		}
		
		/* we need to conceal wether we have access to the requested message so users can't probe us for messages in channels
		 * that they don't have access to. therefore it is important to merge the general case of a wrong message handle with the
		 * case of missing permissions
		 */
		if (ownMessage != null && switch (ownMessage.getChannelType()) {
			case TEXT -> {
				var guild = ownMessage.getGuild();
				var member = guild.getMember(requestee);
				yield ownMessage.getTextChannel().getMembers().contains(member);
			}
			case PRIVATE -> ownMessage.getPrivateChannel().getUser().equals(requestee);
			default -> false;
		}) {
			if (!jda.getSelfUser().equals(ownMessage.getAuthor())) {
				ErrorOutputBuilder.generic("Die Nachricht ist nicht von mir, keine Ahnung woher die kommt.").write(invc).send();
				return;
			}
			
			var maybeSource = service.fetchMessageTrace(ownMessage);
			if (maybeSource.isEmpty()) {
				ErrorOutputBuilder.generic("Ich weiß nicht, warum ich das getan hab. Tut mir leid, bitte nicht hauen.").write(invc).send();
				return;
			}
			var source = maybeSource.get();
			var user = jda.getUserById(source.userId());
			var guildTarget = source.guildId() == 0 ? "@me" : String.valueOf(source.guildId());
			
			var out = invc.reply();
			out.title("Nachrichtenverfolgung");
			var plain = out.description();
			plain.append("[Meine Nachricht](" + ownMessage.getJumpUrl() +")").newLine().newLine();
			plain.append("...war eine Reaktion auf...").newLine().newLine();
			plain.append("[Link zur Nachricht](https://discord.com/channels/%s/%s/%s)".formatted(guildTarget, source.channelId(), source.messageId()));
			if (user != null) {
				plain.newLine().append("Nutzer: ").appendEscape(user.getAsMention());
				out.authorIcon(user.getAvatarUrl());
			}
			plain.newLine().newLine()
					.append("Nachrichteninhalt", ChrislieFormat.BOLD).newLine()
					.appendEscape(source.content(), ChrislieFormat.QUOTE);
			
			out.author("%s%04d".formatted(source.nickname(), source.discriminator()));
			
			var zoneId = CommonFlex.ZONE_ID().getOrFail(invc);
			var formatter = CommonFlex.DATE_TIME_FORMAT().getOrFail(invc);
			out.footer("Nachricht erstellt am: " + formatter.format(TimeUtil.getTimeCreated(source.messageId()).atZoneSameInstant(zoneId)));
			
			out.send();
		} else {
			ErrorOutputBuilder.generic("Ich konnte diese Nachricht leider nicht finden. Sie wurde entweder gelöscht oder du bist nicht berechtigt" +
					" Informationen über diese Nachricht abzufragen.").write(invc).send();
			return;
		}
	}
}
