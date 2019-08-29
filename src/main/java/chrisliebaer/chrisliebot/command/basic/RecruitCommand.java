package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.discord.DiscordService;
import chrisliebaer.chrisliebot.abstraction.irc.IrcService;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Slf4j
public class RecruitCommand implements ChrislieListener.Command {
	
	private Config cfg;
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) {
		return Optional.of("Du willst mich auch in deinem Channel oder Server haben?");
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		if (IrcService.isIrc(invc))
			recruitIrc(invc);
		else if (DiscordService.isDiscord(invc))
			recruitDiscord(invc);
		else
			throw new ListenerException("This command does not know how to handle the current service.");
	}
	
	private void recruitIrc(Invocation invc) throws ListenerException {
		var service = (IrcService) invc.service();
		var arg = invc.arg();
		var allowJoin = invc.ref().flexConf().isSet("recruit.allowJoin");
		String[] args = arg.split(" ", 2);
		
		// irc only allows invite based recruit unless user has permission for join commands
		var client = service.client();
		try {
			if (arg.isEmpty()) {
				invc.reply("Wenn du mich in deinem Channel haben willst, kannst du mich einfach einladen, ich komme dann zu dir.");
			} else {
				if (allowJoin) {
					var user = invc.msg().user();
					if (args.length == 1) {
						log.info("attempting to join {}, triggered by {}", args[0], user.displayName());
						client.addChannel(args[0]);
					} else if (args.length == 2) {
						log.info("attempting to join {} with passwort, triggered by {}", args[0], user.displayName());
						client.addKeyProtectedChannel(args[0], args[1]);
					}
				} else {
					ErrorOutputBuilder.generic("Du bist nicht berechtigt mich in Channel joinen zu lassen.").write(invc).send();
				}
			}
		} catch (IllegalArgumentException ignored) {
			log.warn("channel doesn't exist: {}", args[0]);
			ErrorOutputBuilder.generic("Dieser Channel ist ungültig.").write(invc).send();
		}
	}
	
	private void recruitDiscord(Invocation invc) throws ListenerException {
		var service = (DiscordService) invc.service();
		var reply = invc.reply();
		reply
				.title("Stell mich ein")
				.image(service.jda().getSelfUser().getEffectiveAvatarUrl());
		reply.description()
				.appendEscape("Du willst mich auf auch auf deinem Server haben? Dann lade mich doch ein: ")
				.append(cfg.recruitUrl);
		reply.send();
	}
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	private static class Config {
		
		private @URL @NotNull String recruitUrl;
	}
}
