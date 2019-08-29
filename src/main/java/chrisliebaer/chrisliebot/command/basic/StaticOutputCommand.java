package chrisliebaer.chrisliebot.command.basic;

import chrisliebaer.chrisliebot.abstraction.SerializedOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;

import java.util.Objects;

public class StaticOutputCommand implements ChrislieListener.Command {
	
	private SerializedOutput out;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		out = Objects.requireNonNull(gson.fromJson(json, SerializedOutput.class));
	}
	
	@Override
	public void execute(Invocation invc) throws ListenerException {
		out.apply(invc.reply()).send();
	}
}
