package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.Chrisliebot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 * This class provides a collection of various classes that can be used to uniquely identify abstraction entities of Chrisliebot. All of these implementations can be used
 * as keys for map data structures.
 */
@UtilityClass
public class ChrislieIdentifier {
	
	@ToString
	@EqualsAndHashCode
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class ChannelIdentifier {
		
		private final String service;
		private final String channel;
		
		public static ChannelIdentifier of(ChrislieChannel channel) {
			return new ChannelIdentifier(channel.service().identifier(), channel.identifier());
		}
		
		public Optional<ChrislieChannel> channel(Chrisliebot bot) {
			return bot.service(service).flatMap(service -> service.channel(channel));
		}
	}
}
