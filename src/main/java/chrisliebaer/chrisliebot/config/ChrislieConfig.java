package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.protocol.irc.IrcConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChrislieConfig {
	private String databasePool;
	private List<IrcConfig.BotConfig> irc = new ArrayList<>();
}
