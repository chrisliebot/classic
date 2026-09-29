package chrisliebaer.chrisliebot.abstraction.discord;

import lombok.Data;
import net.dv8tion.jda.api.utils.TimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

/**
 * Contains the trace that caused messages being sent to discord.
 */
@Data
public final class TraceMessageSource {
	
	private long guildId;
	private long channelId;
	private long messageId;
	
	private String nickname;
	private long userId;
	private String content;
	
	private TraceMessageSource(ResultSet rs) throws SQLException {
		guildId = rs.getLong("sourceGuildId");
		channelId = rs.getLong("sourceChannelId");
		messageId = rs.getLong("sourceMessageId");
		
		nickname = rs.getString("sourceUserNickname");
		userId = rs.getLong("sourceUserId");
		
		content = rs.getString("sourceContent");
	}
	
	public OffsetDateTime toInstant() {
		return TimeUtil.getTimeCreated(messageId);
	}
	
	public static TraceMessageSource fromResultSet(ResultSet rs) throws SQLException {
		return new TraceMessageSource(rs);
	}
}
