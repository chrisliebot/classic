package chrisliebaer.chrisliebot.config.scope.selector;

import chrisliebaer.chrisliebot.abstraction.ChrislieChannel;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ChrislieUser;
import chrisliebaer.chrisliebot.config.scope.Selector;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class CombinationSelector implements Selector {
	
	private List<Selector> selectors;
	private BinaryOperator<Boolean> operator;
	
	protected CombinationSelector(List<Selector> selectors, BinaryOperator<Boolean> operator) {
		this.selectors = selectors;
		this.operator = operator;
	}
	
	public static CombinationSelector or(List<Selector> selectors) {
		return new CombinationSelector(selectors, Boolean::logicalOr);
	}
	
	public static CombinationSelector and(List<Selector> selectors) {
		return new CombinationSelector(selectors, Boolean::logicalAnd);
	}
	
	public <T> boolean checkAll(BiFunction<Selector, T, Boolean> fn, T in) {
		return selectors.stream()
				.map(selector -> fn.apply(selector, in)) // map selectors to their state
				.reduce(operator).orElse(false); // operation decides on final value (no short cut option!)
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
