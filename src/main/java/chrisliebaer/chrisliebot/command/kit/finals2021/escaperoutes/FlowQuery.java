package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class FlowQuery implements Comparable<FlowQuery> {
	
	private final Node from, to;
	
	private FlowQuery(Node from, Node to) {
		this.from = from;
		this.to = to;
	}
	
	public static FlowQuery asQuery(Node from, Node to) {
		return new FlowQuery(from, to);
	}
	
	@Override
	public int compareTo(@NotNull FlowQuery o) {
		return from.compareTo(to);
	}
}
