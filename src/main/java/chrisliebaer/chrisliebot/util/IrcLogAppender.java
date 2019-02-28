package chrisliebaer.chrisliebot.util;

import chrisliebaer.chrisliebot.C;
import lombok.NonNull;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.util.Format;

public class IrcLogAppender extends AbstractAppender {
	
	private Client client;
	private String channelName;
	
	public IrcLogAppender(@NonNull Client client, @NonNull String channelName) {
		super("ircAppender", null, null, false, null);
		this.client = client;
		this.channelName = channelName;
	}
	
	@Override
	public void append(LogEvent ev) {
		var channel = client.getChannel(channelName);
		if (!channel.isPresent())
			return;
		
		if (ev.getMarker() != null
				&& ev.getMarker().getName() != null
				&& C.LOG_IRC.getName().equals(ev.getMarker().getName())) {
			channel.get().sendMultiLineMessage(
					Format.BOLD + "[" + getLevelColor(ev.getLevel()) + ev.getLevel().name() + Format.RESET + Format.BOLD + "] " + Format.RESET
							+ C.escapeNickname(channel.get(), ev.getMessage().getFormattedMessage()));
		}
	}
	
	private static Format getLevelColor(Level level) {
		if (level == Level.ERROR)
			return Format.RED;
		if (level == Level.WARN)
			return Format.YELLOW;
		if (level == Level.INFO)
			return Format.GREEN;
		if (level == Level.DEBUG)
			return Format.BLUE;
		if (level == Level.TRACE)
			return Format.LIGHT_GRAY;
		return Format.UNDERLINE;
	}
}
