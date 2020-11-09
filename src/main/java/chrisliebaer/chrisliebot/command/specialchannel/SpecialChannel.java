package chrisliebaer.chrisliebot.command.specialchannel;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.SerializedOutput;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordMessage;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordOutput;
import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.text.StringSubstitutor;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;


// TODO: emote only channel
public class SpecialChannel implements ChrislieListener {
	
	private Config cfg;
	private Chrisliebot bot;
	private List<Predicate<Message>> predicates;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		predicates = new ArrayList<>();
		
		if (cfg.file)
			predicates.add(message -> !message.getAttachments().isEmpty());
		
		if (cfg.link)
			predicates.add(message -> {
				var extractor = LinkExtractor.builder().linkTypes(EnumSet.of(LinkType.URL)).build();
				var links = extractor.extractLinks(message.getContentRaw());
				return links.iterator().hasNext();
			});
		
		if (cfg.pattern != null)
			predicates.add(message -> cfg.pattern.asPredicate().test(message.getContentRaw()));
	}
	
	/*
		muss enthalten:
			regex
			datei
			link
		darf nicht enthalten:
			regex
			datei
			link
	 	modus:
	 		and
	 		or
	 	nachricht:
	 		{user} bitte nicht
	 	ignoreRoles:
	 */
	
	@Override
	public void onMessage(ListenerMessage msg, boolean isCommand) throws ListenerException {
		if (!DiscordService.isDiscord(msg))
			return;
		
		DiscordMessage message = (DiscordMessage) msg.msg();
		var ev = message.ev();
		
		// ignore member on whitelist
		for (Role role : ev.getMember().getRoles()) {
			if (cfg.whitelist.contains(role.getIdLong())) {
				return;
			}
		}
		
		boolean match;
		if (cfg.match == Match.ANY_MATCH)
			match = predicates.stream().anyMatch(p -> p.test(ev.getMessage()));
		else if (cfg.match == Match.ALL_MATCH)
			match = predicates.stream().allMatch(p -> p.test(ev.getMessage()));
		else
			throw new Error("match state is not set");
		
		if (match ? cfg.mode == Mode.DENY : cfg.mode == Mode.ALLOW) {
			// delete message
			message.ev().getMessage().delete().queue();
			
			StringSubstitutor substitutor = new StringSubstitutor(key -> switch (key) {
				case "server" -> ev.getGuild().getName();
				case "mention" -> ev.getAuthor().getAsMention();
				case "user" -> ev.getAuthor().getName();
				case "channel" -> ev.getChannel().getName();
				default -> key;
			});
			DiscordOutput out = (DiscordOutput) cfg.output.apply(
					cfg.sendDm ?
							new DiscordOutput(ev.getAuthor().openPrivateChannel().complete())
							: msg.reply(), substitutor::replace);
			
			var future = out.discordSend();
			
			if (cfg.deleteAfter > 0 && !cfg.sendDm) {
				// register listener to delete message afterwards
				future.thenAccept(m -> bot.sharedResources().timer().schedule(
						() -> m.delete().queue(),
						cfg.deleteAfter, TimeUnit.MILLISECONDS));
			}
		}
	}
	
	private static class Config {
		
		@NotNull private Mode mode; // action to perform on match
		@NotNull private Match match;
		private boolean link;
		private boolean file;
		private Pattern pattern;
		@PositiveOrZero private long deleteAfter; // 0 will disable removal of message
		private boolean sendDm; // sends dm instead of writing in channel (
		@NotNull private SerializedOutput output; // supports ${server} ${mention} ${user} ${channel}
		@NotNull private List<Long> whitelist = List.of(); // role ids to be excluded
	}
	
	private enum Match {
		ALL_MATCH, ANY_MATCH
	}
	
	private enum Mode {
		ALLOW, DENY
	}
}
