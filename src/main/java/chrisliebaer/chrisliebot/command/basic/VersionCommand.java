package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.ChrislieContext;
import chrisliebaer.chrisliebot.util.VersionUtil;

import java.util.Optional;

public class VersionCommand implements ChrislieListener.Command {
	
	@Override
	public Optional<String> help(ChrislieContext ctx, ListenerReference ref) throws ListenerException {
		return Optional.of("Zeigt Informationen zur aktuellen Version von Chrisliebot an.");
	}
	
	@SuppressWarnings({"UnnecessaryUnicodeEscape", "RedundantSuppression"})
	@Override
	public void execute(Invocation invc) throws ListenerException {
		var reply = invc.reply();
		reply.title("Versionsinformationen");
		reply.field("Version", VersionUtil.version());
		reply.field("Branch", VersionUtil.branch());
		
		String clean = "\u2753";
		switch (VersionUtil.clean()) {
			case "true":
				clean = "\u2705";
				break;
			case "false":
				clean = "\u274C";
				break;
		}
		
		reply.field("Clean Build", clean);
		reply.footer("Commit Id: " + VersionUtil.commit());
		
		reply.replace("Aktuelle Version: " + VersionUtil.shortVersion());
		
		reply.send();
	}
}
