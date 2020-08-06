package chrisliebaer.chrisliebot.abstraction.discord;


import chrisliebaer.chrisliebot.abstraction.PlainOutputImpl;
import lombok.NonNull;
import net.dv8tion.jda.api.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static net.dv8tion.jda.api.entities.Message.MentionType.*;

/**
 * This class extends the regular {@link PlainOutputImpl} class by tracking which native mention strings are passed via
 * escaped and unescaped calls respectively. The collected mentions are then used to build a list of allowed mentions
 * that are provided to the Discord API to prevent unwanted mentions without having to do the escaping ourself.
 */
public class DiscordPlainOutput extends PlainOutputImpl {
	
	private final List<Consumer<MessageBuilder>> mentionsTransformers = new ArrayList<>();
	
	public DiscordPlainOutput(@NonNull Function<String, String> escaper, @NonNull BiFunction<Object, String, String> formatResolver) {
		super(escaper, formatResolver);
	}
	
	/**
	 * Applies the rules that were gathered by this output instance to the given message builder.
	 *
	 * @param mb The message builder that's mentions should be configured by this output instance.
	 */
	public void applyMentionRules(MessageBuilder mb) {
		mentionsTransformers.forEach(t -> t.accept(mb));
	}
	
	@Override
	public DiscordPlainOutput append(String s, Object... format) {
		if (EVERYONE.getPattern().matcher(s).find())
			mentionsTransformers.add(mb -> mb.allowMentions(EVERYONE));
		
		if (HERE.getPattern().matcher(s).find())
			mentionsTransformers.add(mb -> mb.allowMentions(HERE));
		
		addMention(s, USER.getPattern(), id -> mentionsTransformers.add(mb -> mb.mentionUsers(id)));
		addMention(s, ROLE.getPattern(), id -> mentionsTransformers.add(mb -> mb.mentionRoles(id)));
		
		super.append(s, format);
		return this;
	}
	
	private void addMention(String s, Pattern pattern, Consumer<String> callback) {
		var matcher = pattern.matcher(s);
		while (matcher.find()) {
			var id = matcher.group(1); // first group is id as string (which is okay since jda takes string as id)
			callback.accept(id);
		}
	}
}
