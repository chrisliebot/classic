package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.function.BiFunction;
import java.util.function.Function;

public class PlainOuputSubstitutionImpl extends PlainOutputImpl implements PlainOutput.PlainOuputSubstitution {
	
	private StrSubstitutor substitutor;
	
	public PlainOuputSubstitutionImpl(@NonNull Function<String, String> escaper,
									  @NonNull BiFunction<Object, String, String> formatResolver,
									  StrLookup lookup) {
		super(escaper, formatResolver);
		substitutor = new StrSubstitutor(lookup);
	}
	
	@Override
	public PlainOuputSubstitution appendSub(String s, Object... format) {
		append(substitutor.replace(s), format);
		return this;
	}
	
	@Override
	public PlainOuputSubstitution appendEscapeSub(String s, Object... format) {
		appendEscape(substitutor.replace(s), format);
		return this;
	}
	
	@Override
	public PlainOuputSubstitutionImpl append(String s, Object... format) {
		super.append(s, format);
		return this;
	}
	
	@Override
	public PlainOuputSubstitutionImpl appendEscape(String s, Object... format) {
		super.appendEscape(s, format);
		return this;
	}
	
	@Override
	public PlainOuputSubstitutionImpl newLine() {
		super.newLine();
		return this;
	}
	
	@Override
	public PlainOuputSubstitutionImpl clear() {
		super.clear();
		return this;
	}
}
