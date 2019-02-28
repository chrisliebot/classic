package chrisliebaer.chrisliebot.abstraction;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.config.ConfigContext;
import lombok.NonNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.util.CtcpUtil;

import java.util.Optional;

public class PrivateMessage implements Message {
	
	private PrivateMessageEvent ev;
	private ConfigContext ctx;
	
	public PrivateMessage(@NonNull PrivateMessageEvent ev, @NonNull ConfigContext ctx) {
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
		
		if (!CtcpUtil.isCtcp(s))
			s = C.ZERO_WIDTH_NO_BREAK_SPACE + s;
		
		ev.getActor().sendMultiLineMessage(C.sanitizeForSend(s));
	}
	
	@Override
	public Optional<Channel> channel() {
		return Optional.empty();
	}
}
