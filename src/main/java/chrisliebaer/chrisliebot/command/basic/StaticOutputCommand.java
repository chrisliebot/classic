package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticOutputCommand implements ChrisieCommand {
	
	private String text;
	
	@Override
	public void execute(ChrislieMessage m, String arg) {
		m.reply(text);
	}
	
	public static StaticOutputCommand fromJson(Gson gson, JsonElement json) {
		return new StaticOutputCommand(json.getAsString());
	}
}
