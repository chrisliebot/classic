package chrisliebaer.chrisliebot.command;

import chrisliebaer.chrisliebot.config.AliasSet;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
