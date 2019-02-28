package chrisliebaer.chrisliebot.listener;

import chrisliebaer.chrisliebot.abstraction.Message;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;

import java.util.regex.Pattern;

@AllArgsConstructor
public class RegexListener implements ChatListener {
	
	private Pattern pattern;
	
	@Override
	public boolean test(Message m) {
		return pattern.asPredicate().test(m.message());
	}
	
	public static RegexListener fromJson(Gson gson, JsonElement json) {
		return new RegexListener(Pattern.compile(json.getAsString()));
	}
}
