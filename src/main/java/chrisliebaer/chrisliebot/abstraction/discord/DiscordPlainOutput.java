package chrisliebaer.chrisliebot.abstraction.discord;


import chrisliebaer.chrisliebot.abstraction.PlainOutputImpl;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
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

	private final Set<MentionType> allowedMentions = EnumSet.noneOf(MentionType.class);
	private final Set<String> mentionedUsers = new HashSet<>();
	private final Set<String> mentionedRoles = new HashSet<>();

	public DiscordPlainOutput(@NonNull Function<String, String> escaper, @NonNull BiFunction<Object, String, String> formatResolver) {
		super(escaper, formatResolver);
	}

	/**
	 * Applies the rules that were gathered by this output instance to the given message builder. All other mentions
	 * are blocked.
	 *
	 * @param mb The message builder that's mentions should be configured by this output instance.
	 */
	public void applyMentionRules(MessageCreateBuilder mb) {
		mb.setAllowedMentions(allowedMentions);
		mb.mentionUsers(mentionedUsers);
		mb.mentionRoles(mentionedRoles);
	}

	@Override
	public DiscordPlainOutput append(String s, Object... format) {
		if (EVERYONE.getPattern().matcher(s).find())
			allowedMentions.add(EVERYONE);

		if (HERE.getPattern().matcher(s).find())
			allowedMentions.add(HERE);

		addMention(s, USER.getPattern(), mentionedUsers::add);
		addMention(s, ROLE.getPattern(), mentionedRoles::add);

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
