package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordGuild;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Optional;

//TODO: put global and other guild rename behind permission
public class NickCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Ändert den Nickname der Botinstanz. Je nach Servicetyp wird der Nickname nur in der aktuellen Community oder global geändert.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (IrcService.isIrc(invc))
			changeIrcNick(invc);
		else if (DiscordService.isDiscord(invc))
			changeDiscordNick(invc);
		else
			throw new ListenerException("This command does not know how to handle the current service.");
	}
	
	private void changeIrcNick(Invocation invc) {
		var service = (IrcService) invc.service();
		service.client().setNick(invc.arg());
	}
	
	private void changeDiscordNick(Invocation invc) throws ListenerException {
		var service = (DiscordService) invc.service();
		var jda = service.jda();
		var arg = invc.arg();
		var args = arg.split(" ", 2);
		var guildId = args[0];
		var reply = invc.reply();
		
		// check if first word matches guild (specified guild takes precedence over current guild)
		Optional<Guild> guild = isValidDiscordGuildIdentifier(guildId) ? Optional.ofNullable(jda.getGuildById(guildId)) : Optional.empty();
		if (guild.isPresent()) {
			// make argument string use remaining string or empty string, if no additional input was given (will clear nick on guilds)
			arg = args.length == 2 ? args[1] : "";
		} else { // otherwise check if command is executed in guild
			guild = guild.or(() -> invc.msg().channel().guild().map(o -> ((DiscordGuild) o).guild()));
		}
		
		var member = guild.map(g -> g.getMember(jda.getSelfUser()));
		
		// empty argument means clearing nickname if in guild
		if (arg.isBlank() && member.isPresent()) {
			member.get().modifyNickname(null).queue(a -> {}, t -> ErrorOutputBuilder.throwable(t).write(reply).send());
			return;
		}
		
		// check for valid nickname
		if (arg.length() < 2 || arg.length() > 32) { // range of allowed nickname length
			ErrorOutputBuilder.generic("Dieser Nickname ist ungültig.").write(reply).send();
			return;
		}
		
		final var finalArg = arg;
		member.map(m -> m.modifyNickname(finalArg))
				.orElseGet(() -> jda.getSelfUser().getManager().setName(finalArg))
				.queue(a -> {}, t -> ErrorOutputBuilder.throwable(t).write(reply).send());
	}
	
	private static boolean isValidDiscordGuildIdentifier(String s) {
		return (!s.isBlank()) && C.isLongParseable(s);
	}
}
