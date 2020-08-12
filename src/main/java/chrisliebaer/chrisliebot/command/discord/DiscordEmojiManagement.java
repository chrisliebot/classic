package chrisliebaer.chrisliebot.command.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordGuild;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.parser.ChrislieParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class DiscordEmojiManagement implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Setzt die Whiteslist für den angegebenen Emoji. Beispiel: :kappa: @Premium @Mitglieder. " +
				"Keine Angabe von Rollen löscht die Whitelist.");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (!DiscordService.isDiscord(invc))
			throw new ListenerException("This command only works on Discord services.");
		
		try {
			var message = (DiscordMessage) invc.msg();
			var maybeGuild = message.channel().guild().map(g -> (DiscordGuild) g);
			if (maybeGuild.isEmpty()) {
				ErrorOutputBuilder.generic("Dieser Befehl kann nur in einer Gilde ausgeführt werden.").write(invc).send();
				return;
			}
			var guild = maybeGuild.get().guild();
			var parser = new ChrislieParser(invc.arg());
			
			// read emoji we want to modify
			var argEmote = parser.word(true).consume().expect("Emoji Mention");
			var maybeEmote = resolve(argEmote, Message.MentionType.EMOTE.getPattern(),
					guild::getEmoteById, s -> guild.getEmotesByName(s, false), 2);
			
			if (maybeEmote.isEmpty()) {
				ErrorOutputBuilder.generic("Ich weiß leider nicht auf welchen Emote du dich beziehst oder deine Auswahl ist doppeldeutig.").write(invc).send();
				return;
			}
			var emote = maybeEmote.get();
			
			var roles = new HashSet<Role>();
			while (parser.word(true).canRead()) {
				var input = parser.word(true).consume().expect();
				
				var role = resolve(input, Message.MentionType.ROLE.getPattern(),
						guild::getRoleById, s -> guild.getRolesByName(s, false), 1);
				
				if (role.isEmpty()) {
					ErrorOutputBuilder.generic("Keine Ahnung welche Rolle das ist: " + input).write(invc).send();
					return;
				}
				
				roles.add(role.get());
			}
			
			// update emote
			emote.getManager().setRoles(roles.isEmpty() ? null : roles).submit();
			
			var out = invc.reply();
			out.title("Rollenwhiteliste für " + emote.getAsMention() + " angepasst.");
			var desc = out.description();
			if (roles.isEmpty()) {
				desc.appendEscape("Keine Rollenbeschränkung festgelegt.", ChrislieFormat.ITALIC);
			} else {
				var joiner = desc.joiner(" ");
				for (Role role : roles) {
					joiner.seperator();
					joiner.append(role.getAsMention());
				}
			}
			out.send();
		} catch (ChrislieParser.ParserException e) {
			ErrorOutputBuilder.generic(e.getMessage()).write(invc).send();
		}
	}
	
	private static <T> Optional<T> resolve(String input, Pattern pattern, Function<String, T> idLookup, Function<String, List<T>> nameLookup, int idGroup) {
		// check if user used mention (parameter containts entire mention, so we match on entire input)
		var matcher = pattern.matcher(input);
		if (matcher.matches()) {
			var id = matcher.group(idGroup);
			return Optional.ofNullable(idLookup.apply(id));
		}
		
		// check if user typed id as string
		try {
			var t = idLookup.apply(input);
			if (t != null)
				return Optional.of(t);
		} catch (NumberFormatException ignore) {}
		
		// assume user used name (could lead to duplicates, so we check if name is ambiguous
		var list = nameLookup.apply(input);
		if (list.size() == 1)
			return Optional.of(list.get(0));
		
		return Optional.empty();
	}
}
