package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.command.ListenerReference;
import chrisliebaer.chrisliebot.config.flex.FlexConf;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@ToString
public class ChrislieGroup {
	
	@Getter private String name;
	@Getter @ToString.Exclude private FlexConf flexConf;
	
	@Getter @ToString.Exclude private Collection<ChrislieGroup> includes;
	@Getter @ToString.Exclude private Map<String, ListenerReference> refs;
	
	public ChrislieGroup(String name, FlexConf flexConf, Collection<ChrislieGroup> includes, Collection<ListenerReference> refs) {
		this.name = name;
		this.flexConf = flexConf;
		this.includes = includes;
		
		// collect references in hashmap for quick lookup
		this.refs = new HashMap<>(refs.size());
		for (ListenerReference ref : refs)
			this.refs.put(ref.name(), ref);
	}
}
