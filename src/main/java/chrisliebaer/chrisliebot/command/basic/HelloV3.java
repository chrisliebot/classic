package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

public class HelloV3 implements ChrislieListener.Command {
	
	private String out;
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		invc.reply(out);
	}
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) {
		out = json.getAsString();
	}
}
