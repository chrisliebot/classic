package chrisliebaer.chrisliebot.config.flex;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.NonNull;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * This class provides a simple map-like data structure for looking up arbitrary values. Multiple instances of this
 * class can be combined by calling {@link #apply(FlexConf)} without modifying the underlining data structure, meaning
 * that each instance that was merged into another instance will still operate the same way it did before the merge.
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
public class FlexConf {
	
	public static final String DISPATCHER_PATTERN = "dispatcher.pattern";
	public static final String DISPATCHER_DISABLE = "dispatcher.disable";
	public static final String DISPATCHER_VERBOSE = "dispatcher.verbose";
	
	private Resolver resolver;
	
	/**
	 * Creates a new FlexConf without any resolver, meaning that any lookup will fail unless other flex confs are merged
	 * into this one by calling {@link #apply(FlexConf)}.
	 */
	public FlexConf() {
		this.resolver = new Resolver() {};
	}
	
	/**
	 * Creates a new FlexConf with the given resolver.
	 *
	 * @param resolver A resolver instance that will be used by the newly created FlexConfg.
	 */
	public FlexConf(@NonNull Resolver resolver) {
		this.resolver = resolver;
	}
	
	/**
	 * Creates a new FlexConf that will use the provided FlexConfg as a fallback including all changes that were
	 * performed on the given FlexConf after this method was called.
	 *
	 * @param o The FlexConfg that will act as a fallback.
	 * @return A new FlexConf that can be modified without changing the state of the given FlexConf.
	 */
	public static FlexConf fallback(@NonNull FlexConf o) {
		return new FlexConf(new Resolver() {
			@Override
			public <V> Optional<V> get(String key, Class<V> clazz) {
				return o.resolver.get(key, clazz);
			}
			
			@Override
			public Optional<Object> get(String key, Type type) {
				return o.resolver.get(key, type);
			}
		});
	}
	
	/**
	 * Creates a new FlexConfg that is a snapshot of the provided one, meaning that changes in the given FlexConf will
	 * not be reflected by the returned instance.
	 *
	 * @param o The FlexConf to snapshot.
	 * @return A snapshot of the provided FlexConf.
	 */
	public static FlexConf snapshot(FlexConf o) {
		// it's actually enought to just copy the resolver
		return new FlexConf(o.resolver);
	}
	
	/**
	 * This method links the given FlexConf into this one, meaning that any lookup to this FlexConf will first check the
	 * given FlexConf before falling back to this one. Note that this is opposite from what you might expect. The given
	 * FlexConf will NOT act as a fallback but instead this FlexConf will become the fallback.
	 *
	 * @param o The FlexConf that will prececde this FlexConf.
	 * @return This flex conf for method chaining.
	 */
	public FlexConf apply(@NonNull FlexConf o) {
		// this creates a chain of resolver, with the current one being in front
		resolver = new ChainResolver(o.resolver, resolver); // yes, this is the correct parameter order, new one goes first and becomes current
		return this;
	}
	
	public OptionalInt getInteger(String key) {
		return resolver.get(key, Integer.class).map(OptionalInt::of).orElseGet(OptionalInt::empty);
	}
	
	public int getIntegerOrFail(String key) throws ChrislieListener.ListenerException {
		return getInteger(key).orElseThrow(() -> keyNotFound(key));
	}
	
	public OptionalLong getLong(String key) {
		return resolver.get(key, Long.class).map(OptionalLong::of).orElseGet(OptionalLong::empty);
	}
	
	public long getLongOrFail(String key) throws ChrislieListener.ListenerException {
		return getLong(key).orElseThrow(() -> keyNotFound(key));
	}
	
	public OptionalDouble getDouble(String key) {
		return resolver.get(key, Double.class).map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
	}
	
	public double getDoubleorFail(String key) throws ChrislieListener.ListenerException {
		return getDouble(key).orElseThrow(() -> keyNotFound(key));
	}
	
	public Optional<String> getString(String key) {
		return resolver.get(key, String.class);
	}
	
	public String getStringOrFail(String key) throws ChrislieListener.ListenerException {
		return getString(key).orElseThrow(() -> keyNotFound(key));
	}
	
	public boolean isSet(String key) {
		return get(key, Boolean.class).orElse(false);
	}
	
	public <V> Optional<V> get(String key, Class<V> clazz) {
		return resolver.get(key, clazz);
	}
	
	public <V> V getOrFail(String key, Class<V> clazz) throws ChrislieListener.ListenerException {
		return get(key, clazz).orElseThrow(() -> keyNotFound(key));
	}
	
	public <T> Optional<T> get(String key, Type type) {
		return C.unsafeCast(resolver.get(key, type));
	}
	
	public <T> T getOrFail(String key, Type type) throws ChrislieListener.ListenerException {
		return C.unsafeCast(get(key, type).orElseThrow(() -> keyNotFound(key)));
	}
	
	protected static ChrislieListener.ListenerException keyNotFound(String string) {
		return new ChrislieListener.ListenerException("key not found in flexconf: " + string);
	}
	
	/**
	 * The resolver is responsible for providing the lookup code. Note that resolvers are free to cache values, meaning
	 * that multiple lookups for the same key may or may not return the same instance.
	 */
	public static interface Resolver {
		
		public default <V> Optional<V> get(String key, Class<V> clazz) {
			return Optional.empty();
		}
		
		public default Optional<Object> get(String key, Type type) {
			return Optional.empty();
		}
	}
	
	// this is art
	private static class ChainResolver implements Resolver {
		
		private Resolver current;
		private Resolver next;
		
		public ChainResolver(@NonNull Resolver current, @NonNull Resolver next) {
			this.current = current;
			this.next = next;
		}
		
		@Override
		public <V> Optional<V> get(String key, Class<V> clazz) {
			return current.get(key, clazz).or(() -> next.get(key, clazz));
		}
		
		@Override
		public Optional<Object> get(String key, Type type) {
			return current.get(key, type).or(() -> next.get(key, type));
		}
	}
}
