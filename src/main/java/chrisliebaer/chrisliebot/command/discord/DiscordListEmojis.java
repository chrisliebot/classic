package chrisliebaer.chrisliebot.command.discord;

import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class DiscordListEmojis implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Listet alle Emojis auf, auf die ich aktuell Zugriff habe. Vorsicht: Das sind viele!");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (!DiscordService.isDiscord(invc)) {
			invc.reply("Dieser Befehl funktioniert nur auf Discordinstanzen, das tut mir leid.");
			return;
		}
		
		var service = (DiscordService) invc.service();
		var msg = (DiscordMessage) invc.msg();
		var channel = msg.channel().messageChannel();
		var jda = service.jda();
		var self = jda.getSelfUser();
		
		// some emotes might be limited to certain roles, and can't be posted, but we still track them
		int filtered = 0;
		var list = new ArrayList<Emote>();
		for (var emote : jda.getEmotes()) {
			if (emote.canInteract(self, channel))
				list.add(emote);
			else
				filtered++;
		}
		
		if (list.isEmpty())
			return; // TODO: throw exception once new error system is done
		
		// shuffle in deterministic but semingly random way
		list.sort(Comparator.comparingInt(Object::hashCode));
		
		// set to max so first run will create output
		int page = 0;
		ChrislieOutput out = null;
		PlainOutput description = null;
		int written = MessageEmbed.TEXT_MAX_LENGTH;
		for (var emote : list) {
			var mention = emote.getAsMention();
			
			// send current output and create new one
			if (written + mention.length() > MessageEmbed.TEXT_MAX_LENGTH) {
				
				// don't send first message as it is null
				if (out != null)
					out.send();
				
				out = invc.reply();
				out.title("Emoteliste Seite " + ++page);
				description = out.description();
				written = 0;
			}
			
			// it's save to assume that emote will now fit
			assert description != null;
			description.append(mention);
			written += mention.length();
		}
		
		// send remaining
		assert out != null;
		out.send();
	}
}
