package chrisliebaer.chrisliebot.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.regex.Pattern;

public class PatternTypeAdapter extends TypeAdapter<Pattern> {
	
	@Override
	public void write(JsonWriter out, Pattern value) throws IOException {
		out.value(value.pattern());
	}
	
	@Override
	public Pattern read(JsonReader in) throws IOException {
		return Pattern.compile(in.nextString());
	}
}
