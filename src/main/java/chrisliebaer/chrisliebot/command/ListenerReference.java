package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.config.AliasSet;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import lombok.*;

@ToString
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ListenerReference {
	@Getter protected String name;
	@Getter protected String help;
	
	@Getter protected ChrislieListener.Envelope envelope;
	
	@Getter protected FlexConf flexConf;
	@Getter protected AliasSet aliasSet;
}
