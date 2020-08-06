package chrisliebaer.chrisliebot.util;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.abstractbase.ActorChannelMessageEventBase;
import org.kitteh.irc.client.library.event.abstractbase.ActorPrivateMessageEventBase;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNoticeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.user.PrivateCtcpQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.event.user.UserNickChangeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

@Slf4j
public class IrcToSqlLogger {
	
	public enum MessageType {
		NORMAL, CTCP, NOTICE, JOIN, PART, QUIT, NICK, KICK
	}
	
	private DataSource dataSource;
	
	public IrcToSqlLogger(@NonNull DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	private void ensureEncoding(Connection conn) {
		try (var stmt = conn.prepareStatement("SET NAMES 'utf8mb4'")) {
			stmt.execute();
		} catch (SQLException e) {
			log.error("failed to set sql connection to utf8mb4, emojis might cause errors", e);
		}
	}
	
	@Handler
	public void logChannel(ActorChannelMessageEventBase<User> ev) {
		MessageType type = null;
		if (ev instanceof ChannelCtcpEvent) {
			type = MessageType.CTCP;
		} else if (ev instanceof ChannelMessageEvent) {
			type = MessageType.NORMAL;
		} else if (ev instanceof ChannelNoticeEvent) {
			type = MessageType.NOTICE;
		}
		
		if (type != null)
			logMessage(new Date(), ev.getChannel().getName(), ev.getActor(), ev.getMessage(), type);
	}
	
	@Handler
	public void logJoin(ChannelJoinEvent ev) {
		logMessage(new Date(), ev.getChannel().getName(), ev.getUser(), null, MessageType.JOIN);
	}
	
	@Handler
	public void logPart(ChannelPartEvent ev) {
		logMessage(new Date(), ev.getChannel().getName(), ev.getUser(), null, MessageType.PART);
	}
	
	@Handler
	public void logQuit(UserQuitEvent ev) {
		if (ev.getAffectedChannel().isPresent()) {
			logMessage(new Date(), ev.getAffectedChannel().get().getName(), ev.getUser(), null, MessageType.QUIT);
		} else {
			logMessage(new Date(), ev.getUser().getNick(), ev.getUser(), null, MessageType.QUIT);
		}
	}
	
	@Handler
	public void logQuery(ActorPrivateMessageEventBase<User> ev) {
		if (ev instanceof PrivateMessageEvent) {
			logMessage(new Date(), ev.getTarget(), ev.getActor(), ev.getMessage(), MessageType.NORMAL);
		} else if (ev instanceof PrivateCtcpQueryEvent) {
			logMessage(new Date(), ev.getTarget(), ev.getActor(), ev.getMessage(), MessageType.CTCP);
		} else if (ev instanceof PrivateNoticeEvent) {
			logMessage(new Date(), ev.getTarget(), ev.getActor(), ev.getMessage(), MessageType.NOTICE);
		}
	}
	
	@Handler
	public void logNick(UserNickChangeEvent ev) {
		logMessage(new Date(), ev.getOldUser().getNick(), ev.getOldUser(), ev.getSource().getMessage(), MessageType.NICK);
	}
	
	@Handler
	public void logKick(ChannelKickEvent ev) {
		logMessage(new Date(), ev.getChannel().getName(), ev.getUser(), ev.getMessage() + "(" + ev.getTarget().getNick() + ")", MessageType.KICK);
	}
	
	private synchronized void logMessage(Date when, String context, User sender, String message, MessageType type) {
		log.trace("LOG: {} [{}] {}: {} ({})", when, context, sender.getNick(), message, type);
		
		try {
			String sql = "INSERT INTO chatlog(timestamp, context, type, nickname, realname, ident, host, account, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (Connection connection = dataSource.getConnection();
				 var stmt = connection.prepareStatement(sql)) {
				
				ensureEncoding(connection);
				
				stmt.setTimestamp(1, new Timestamp(when.getTime()));
				stmt.setString(2, context);
				stmt.setString(3, type.name());
				stmt.setString(4, sender.getNick());
				stmt.setString(5, sender.getRealName().orElse(null));
				stmt.setString(6, sender.getUserString());
				stmt.setString(7, sender.getHost());
				stmt.setString(8, sender.getAccount().orElse(null));
				stmt.setString(9, message);
				stmt.execute();
			}
		} catch (SQLException e) {
			log.warn("error while logging message", e);
		}
	}
	
	@Data
	@Builder
	private static class LogRecord {
		
		private Timestamp timestamp; // find out type
		private String nickname;
		private String message;
		private MessageType type;
	}
}
