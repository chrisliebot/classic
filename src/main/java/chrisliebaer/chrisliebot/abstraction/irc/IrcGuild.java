package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieGuild;
import lombok.Getter;
import org.kitteh.irc.client.library.element.Channel;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IrcGuild implements ChrislieGuild {
	
	@Getter private IrcService service;
	@Getter private String identifier;
	
	private List<Channel> channels;
	
	public IrcGuild(IrcService service, String identifier, List<Channel> channels) {
		this.service = service;
		this.identifier = identifier;
		this.channels = channels;
	}
	
	@Override
	public String displayName() {
		return identifier;
	}
	
	@Override
	public Collection<IrcChannel> channels() {
		return channels.stream()
				.map(channel -> new IrcChannel(service, channel, this))
				.collect(Collectors.toList());
	}
}
