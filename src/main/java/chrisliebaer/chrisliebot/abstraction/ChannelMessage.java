package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.config.ConfigContext;
import lombok.NonNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.util.Optional;

public class ChannelMessage implements Message {
	
	private ChannelMessageEvent ev;
	private ConfigContext ctx;
	
	public ChannelMessage(@NonNull ChannelMessageEvent ev, @NonNull ConfigContext ctx) {
		this.ev = ev;
		this.ctx = ctx;
	}
	
	@Override
	public User user() {
		return ev.getActor();
	}
	
	@Override
	public boolean isAdmin() {
		return ctx.isAdmin(user());
	}
	
	@Override
	public String message() {
		return ev.getMessage();
	}
	
	@Override
	public void reply(String s) {
		C.sendChannelMessage(ev.getChannel(), s);
	}
	
	@Override
	public Optional<Channel> channel() {
		return Optional.of(ev.getChannel());
	}
}
