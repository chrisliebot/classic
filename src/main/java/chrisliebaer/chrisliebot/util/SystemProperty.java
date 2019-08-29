package chrisliebaer.chrisliebot.util;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.function.Function;

@UtilityClass
public class SystemProperty<T> {
	
	private static <T> T get(@NonNull String name, Function<String, T> fn, T def) {
		Preconditions.checkArgument(!name.isBlank(), "name must not be blank");
		var v = System.getProperty(name);
		return v == null ? def : fn.apply(v);
	}
	
	public static Optional<String> of(String name) {
		return get(name, Optional::of, Optional.empty());
	}
	
	public static String of(String name, String def) {
		return get(name, Function.identity(), def);
	}
	
	public static <V> Optional<V> of(String name, Function<String, V> convert) {
		return get(name, s -> Optional.of(s).map(convert), Optional.empty());
	}
	
	public static <V> V of(String name, V def, Function<String, V> convert) {
		return get(name, convert, def);
	}
}
