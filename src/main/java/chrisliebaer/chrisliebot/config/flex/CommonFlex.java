package chrisliebaer.chrisliebot.config.flex;

import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiFunction;

@UtilityClass
public class CommonFlex {
	
	@Getter private static final Provider<DateTimeFormatter> DATE_TIME_FORMAT = provider(
			(flex, key) -> flex.getString(key).map(DateTimeFormatter::ofPattern), "chrisliebot.dateTimeFormat");
	
	@Getter private static final Provider<DateTimeFormatter> TIME_FORMAT = provider(
			(flex, key) -> flex.getString(key).map(DateTimeFormatter::ofPattern), "chrisliebot.timeFormat");
	
	@Getter private static final Provider<DateTimeFormatter> DATE_FORMAT = provider(
			(flex, key) -> flex.getString(key).map(DateTimeFormatter::ofPattern), "chrisliebot.dateFormat");
	
	@Getter private static final Provider<ZoneId> ZONE_ID = provider(
			(flex, key) -> flex.getString(key).map(ZoneId::of), "chrisliebot.zoneId");
	
	
	private static final class ProviderImpl<T> implements Provider<T> {
		
		private String key;
		private BiFunction<FlexConf, String, Optional<T>> convert;
		
		private ProviderImpl(@NonNull BiFunction<FlexConf, String, Optional<T>> convert, @NonNull String key) {
			this.convert = convert;
			this.key = key;
		}
		
		@Override
		public Optional<T> get(FlexConf flex) {
			return convert.apply(flex, key);
		}
		
		@Override
		public T getOrFail(FlexConf flex) throws ChrislieListener.ListenerException {
			return convert.apply(flex, key).orElseThrow(() -> FlexConf.keyNotFound(key));
		}
	}
	
	public static <T> Provider<T> provider(BiFunction<FlexConf, String, Optional<T>> fn, String key) {
		return new ProviderImpl<>(fn, key);
	}
	
	public static interface Provider<T> {
		
		public Optional<T> get(FlexConf flex);
		
		public T getOrFail(FlexConf flex) throws ChrislieListener.ListenerException;
	}
}
