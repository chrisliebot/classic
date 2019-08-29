package chrisliebaer.chrisliebot.config.scope;

import chrisliebaer.chrisliebot.config.ChrislieGroup;
import chrisliebaer.chrisliebot.config.scope.selector.CombinationSelector;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;

public class ScopeMapping extends CombinationSelector {
	
	@Getter private List<ChrislieGroup> groups;
	
	public ScopeMapping(List<Selector> selectors, List<ChrislieGroup> groups) {
		super(selectors, Boolean::logicalAnd);
		this.groups = ImmutableList.copyOf(groups);
	}
}
