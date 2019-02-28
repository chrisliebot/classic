package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.config.ConfigContext;
import lombok.NonNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.util.CtcpUtil;
import org.kitteh.irc.client.library.util.Format;

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
		if (s == null || s.isEmpty())
			return;
		
		Channel channel = ev.getChannel();
		
		// strip formatting if channel doesn't support it
		if (!C.channelSupportsFormatting(channel))
			s = Format.stripAll(s);
		
		if (!CtcpUtil.isCtcp(s))
			s = C.ZERO_WIDTH_NO_BREAK_SPACE + s;
		
		ev.getChannel().sendMultiLineMessage(C.sanitizeForSend(C.escapeNickname(channel, s)));
	}
	
	@Override
	public Optional<Channel> channel() {
		return Optional.of(ev.getChannel());
	}
}
