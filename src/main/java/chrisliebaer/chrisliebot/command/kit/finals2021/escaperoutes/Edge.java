package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class Edge implements Comparable<Edge> {
	
	private final Node from, to;
	
	private Edge(Node from, Node to) {
		this.from = from;
		this.to = to;
	}
	
	public static Edge between(Node from, Node to) {
		return new Edge(from, to);
	}
	
	public Edge reverse() {
		return new Edge(to, from);
	}
	
	@Override
	public int compareTo(@NotNull Edge o) {
		var firstComp = from.compareTo(o.from);
		if (firstComp != 0)
			return firstComp;
		return to.compareTo(o.to);
	}
}
