package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This graph implementation allows building of graphs that are compatible with the algorithm defined in {@link MaxFlowSolver}.
 */
public final class FlowGraph {
	
	private final Map<Edge, Integer> edges = new HashMap<>();
	private final Set<Node> nodes = new HashSet<>();
	
	private FlowGraph() {}
	
	/**
	 * @return An empty graph, duh.
	 */
	public static FlowGraph empty() {
		return new FlowGraph();
	}
	
	/**
	 * Converts the current graph into it's map representation with implicit node declarations.
	 *
	 * @return The edge map representing this graph.
	 */
	public Map<Edge, Integer> toEdgeMap() {
		return Map.copyOf(edges);
	}
	
	/**
	 * @return The number of nodes in this graph.
	 */
	public int nodeCount() {
		return nodes.size();
	}
	
	/**
	 * @param node The node to check for.
	 * @return Wether this graph contains the given node.
	 */
	public boolean contains(Node node) {
		return nodes.contains(node);
	}
	
	/**
	 * Updates the given edge to allow flow of given capacity.
	 * <p>
	 * Graph is implicitly modelled as complete graph with positive edge capacity where actual edges are. Setting an edge to {@code 0} will therefore remove edge.
	 *
	 * @param edge     The edge to modify.
	 * @param capacity The new capacity of the given edge. Must not pe negative.
	 * @throws IllegalGraphException If new edge capacity would be violating graph contract.
	 */
	public void modifyEdge(Edge edge, int capacity) throws IllegalGraphException {
		// check for loop
		if (edge.from().equals(edge.to()))
			throw new IllegalGraphException("loops are not permitted");
		
		// check for backwards edge
		if (edges.containsKey(edge.reverse()))
			throw new IllegalGraphException("already contains reverse edge");
		
		// check for negative capacity
		if (capacity < 0)
			throw new IllegalGraphException("negative edge capacity");
		
		if (capacity == 0)
			edges.remove(edge);
		else
			edges.put(edge, capacity);
		
		// required for node count
		nodes.add(edge.from());
		nodes.add(edge.to());
	}
}
