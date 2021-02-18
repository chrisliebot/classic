package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Node implements Comparable<Node> {
	
	private final String name;
	
	public static Node withName(String name) {
		return new Node(name);
	}
	
	@Override
	public int compareTo(@NotNull Node o) {
		return name.compareTo(o.name);
	}
}
