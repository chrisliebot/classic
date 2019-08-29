package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PlainOutputSubstituionImpl extends PlainOutputImpl implements PlainOutput.PlainOutputSubstituion {
	
	private StrSubstitutor substitutor;
	
	/* substitution pattern might be specified before values subsitution map has been fully build
	 * therefore we have to delay the actual lookup until the very end by tracking the input calls
	 */
	private List<Runnable> calls = new ArrayList<>();
	
	public PlainOutputSubstituionImpl(@NonNull Function<String, String> escaper,
									  @NonNull BiFunction<Object, String, String> formatResolver,
									  StrLookup lookup) {
		super(escaper, formatResolver);
		substitutor = new StrSubstitutor(lookup);
	}
	
	@Override
	public PlainOutputSubstituion appendSub(String s, Object... format) {
		calls.add(() -> super.append(substitutor.replace(s), format));
		return this;
	}
	
	@Override
	public PlainOutputSubstituion appendEscapeSub(String s, Object... format) {
		calls.add(() -> super.appendEscape(substitutor.replace(s), format));
		return this;
	}
	
	@Override
	public PlainOutputSubstituionImpl append(String s, Object... format) {
		calls.add(() -> super.append(s, format));
		return this;
	}
	
	@Override
	public PlainOutputSubstituionImpl appendEscape(String s, Object... format) {
		calls.add(() -> super.appendEscape(s, format));
		return this;
	}
	
	@Override
	public PlainOutputSubstituionImpl newLine() {
		calls.add(super::newLine);
		return this;
	}
	
	@Override
	public String string() {
		// actually resolve lookup here
		calls.forEach(Runnable::run);
		calls.clear(); // clear is required to prevent duplication on second call
		return super.string();
	}
	
	@Override
	public PlainOutputSubstituionImpl clear() {
		super.clear();
		return this;
	}
}
