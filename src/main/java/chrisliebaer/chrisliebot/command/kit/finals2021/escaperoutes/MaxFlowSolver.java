package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class MaxFlowSolver {
	
	private final Map<Edge, Integer> edges = new HashMap<>();
	
	private MaxFlowSolver(Map<Edge, Integer> edges) {
		this.edges.putAll(edges);
	}
	
	public static MaxFlowSolver ofFlowGraph(FlowGraph graph) {
		return new MaxFlowSolver(graph.toEdgeMap());
	}
	
	public int maxFlow(FlowQuery query) {
		var flowState = new HashMap<>(edges);
		
		var maxFlow = 0;
		
		while (true) {
			var path = findPath(flowState.keySet(), query.from(), query.to());
			
			// no path indicates max flow has been reached
			if (path.isEmpty())
				return maxFlow;
			
			// if path is not empty, we have min flow
			var min = path.stream().mapToInt(flowState::get).min().orElseThrow();
			maxFlow += min;
			for (var edge : path) {
				var current = flowState.get(edge); // value exists cause otherwise edge wouldnt exist in residual graph
				
				// update forward and backward edges but remove edges with no capacity
				var forwardFlow = current - min;
				if (forwardFlow > 0) {
					flowState.put(edge, forwardFlow);
				} else {
					flowState.remove(edge);
				}
				
				// backwards edges can only gain capacity
				flowState.put(edge.reverse(), current + min);
			}
		}
	}
	
	private static List<Edge> findPath(Iterable<Edge> edges, Node start, Node finish) {
		// build lookup table for faster iteration
		Multimap<Node, Node> lookup = ArrayListMultimap.create();
		for (Edge edge : edges)
			lookup.put(edge.from(), edge.to());
		
		// keep track of traversal
		var parents = new HashMap<Node, Node>();
		var visited = new HashSet<Node>(lookup.size());
		var pending = new LinkedList<Node>();
		
		// first node is always considered visited
		visited.add(start);
		pending.addFirst(start);
		
		while(!pending.isEmpty()) {
			var node = pending.removeLast();
			
			// we reached final node, backtrack with parent information
			if (node.equals(finish)) {
				var path = new ArrayList<Edge>();
				var current = node;
				while(parents.containsKey(current)) {
					var parent = parents.get(current);
					path.add(Edge.between(parent, current));
					current = parent;
				}
				
				// list is in wrong order, so we reverse it
				return Lists.reverse(path);
			}
			
			// expand node but ignore nodes that have already been visited
			for (Node neighbor : lookup.get(node)) {
				if (!visited.contains(neighbor)) {
					parents.put(neighbor, node);
					pending.addFirst(neighbor);
					visited.add(neighbor);
				}
			}
		}
		
		return List.of();
	}
}
