package chrisliebaer.chrisliebot.protocol.discord;

import lombok.Data;

import java.util.List;

@Data
public class DiscordConfig {
	private String prefix;
	private String token;
	private List<String> admins;
}
