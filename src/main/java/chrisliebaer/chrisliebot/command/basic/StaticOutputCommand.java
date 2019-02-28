package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticOutputCommand implements CommandExecutor {
	
	private String text;
	
	@Override
	public void execute(Message m, String arg) {
		m.reply(text);
	}
	
	public static StaticOutputCommand fromJson(Gson gson, JsonElement json) {
		return new StaticOutputCommand(json.getAsString());
	}
}
