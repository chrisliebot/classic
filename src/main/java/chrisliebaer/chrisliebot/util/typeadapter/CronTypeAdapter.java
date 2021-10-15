package chrisliebaer.chrisliebot.util.typeadapter;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.parser.CronParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.NonNull;

import java.io.IOException;

public class CronTypeAdapter extends TypeAdapter<Cron> {
	
	private final CronDefinition cronDefinition;
	
	public CronTypeAdapter(@NonNull CronDefinition cronDefinition) {
		this.cronDefinition = cronDefinition;
	}
	
	@Override
	public void write(JsonWriter out, Cron value) throws IOException {
		out.value(value.asString());
	}
	
	@Override
	public Cron read(JsonReader in) throws IOException {
		return new CronParser(cronDefinition).parse(in.nextString());
	}
}
