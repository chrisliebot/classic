package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.function.BiFunction;

public class CombinationSelector implements Selector {
	
	private List<Selector> selectors;
	private Operation operation;
	
	protected enum Operation {
		OR, AND
	}
	
	protected CombinationSelector(List<Selector> selectors, Operation operation) {
		Preconditions.checkArgument(!selectors.isEmpty(), "selector list must no be empty");
		
		this.selectors = selectors;
		this.operation = operation;
	}
	
	public static CombinationSelector or(List<Selector> selectors) {
		return new CombinationSelector(selectors, Operation.OR);
	}
	
	public static CombinationSelector and(List<Selector> selectors) {
		return new CombinationSelector(selectors, Operation.AND);
	}
	
	public <T> boolean checkAll(BiFunction<Selector, T, Boolean> fn, T in) {
		return switch (operation) {
			case OR -> checkAllOr(fn, in);
			case AND -> checkAllAnd(fn, in);
		};
	}
	
	public <T> boolean checkAllOr(BiFunction<Selector, T, Boolean> fn, T in) {
		for (var selector : selectors) {
			if (fn.apply(selector, in))
				return true;
		}
		return false;
	}
	
	public <T> boolean checkAllAnd(BiFunction<Selector, T, Boolean> fn, T in) {
		for (var selector : selectors) {
			if (!fn.apply(selector, in))
				return false;
		}
		return true;
	}
	
	@Override
	public boolean check(ChrislieMessage message) {
		return checkAll(Selector::check, message);
	}
	
	@Override
	public boolean check(ChrislieUser user) {
		return checkAll(Selector::check, user);
	}
	
	@Override
	public boolean check(ChrislieChannel channel) {
		return checkAll(Selector::check, channel);
	}
	
	@Override
	public boolean check(ChrislieService service) {
		return checkAll(Selector::check, service);
	}
}
