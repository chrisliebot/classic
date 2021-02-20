package chrisliebaer.chrisliebot.command.kit.finals2021.escaperoutes;

import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.util.ErrorOutputBuilder;
import chrisliebaer.chrisliebot.util.parser.ChrislieParser;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This command implements the first of the two final assigments from the Karlsruher Institut of Technology of the freshmen course "Programmieren". The course was held in
 * winter 2020-2021. This implementation does not strictly follow the required specification but rather implements it the way I would have expected the specification to
 * look like. This primarly includes the handling of excessive whitespaces as well as edge updates which should be more flexible for the sake of easy use in Discord
 * contexts.
 * <p>
 * Sadly the assigment contained very little actual software architecture and focused mostly on implementing an overly complex command line interface that could have been
 * massively simplified by removing ambiguities. Something which I didn't bother sinking time into. It is what it is.
 */
public class KitEscapeRoutesCommand implements ChrislieListener.Command {
	
	private static final int LIMIT_MAX_FLOW_CACHE = 20;
	private static final int LIMIT_MAX_EDGE_COUNT = 100;
	private static final int LIMIT_MAX_GRAPH_COUNT = 30;
	
	private static final Pattern GRAPH_NAME_PATTERN = Pattern.compile("[A-Z]{1,6}");
	private static final Pattern GRAPH_NODE_SPEC_PATTERN = Pattern.compile("(?<from>[a-z]{1,6})(?<capacity>[0-9]+)(?<to>[a-z]{1,6})");
	
	private final Map<String, GraphContainer> graphs = new HashMap<>();
	
	@Override
	public synchronized void execute(Invocation invc) throws ListenerException {
		try {
			var arg = invc.arg();
			var parser = new ChrislieParser(arg);
			var verb = parser.word(true).consume().expect("graph verb");
			
			/* Syntax definition
			 *	<n> name of graph
			 *	<v> name of node
			 *	<k> capacity of edge
			 *	<f> max flow
			 *	<x> number of nodes in graph
			 *	<e> edge with capacity <v1><k><v2>
			 *	<g> graph definition in form `<e1>;<e2>;...;<en>`
			 */
			switch (verb) {
				/* ambiguous command, requires lookahead and graph lookup
				 *	add <n> <g> - create new graph from graph definition
				 *	add <n> <e> - add new edge to existing node or override existing one (bad naming)
				 */
				case "add" -> {
					var name = parser.word(true).consume().expect("graph name");
					var container = graphs.get(name);
					if (container == null)
						actionAddNewGraph(invc, parser, name);
					else
						actionModifyGraph(invc, parser, container);
				}
				/* ambiguous command, requires lookahead
				 *	list - print graph name and number of nodes: <n> <x> or EMPTY
				 *	list <n> - print all cached max flows: <f> <v1> <v2>
				 */
				case "list" -> {
					var nameToken = parser.word(true).peek();
					if (nameToken.isSuccess()) {
						var name = nameToken.expect("graph name");
						var container = graphs.get(name);
						if (container == null) {
							ErrorOutputBuilder.generic("Ich kenn keinen Graphen mit dem Namen " + name).write(invc).send();
							return;
						}
						actionListFlows(invc, container);
					} else {
						actionListGraphs(invc);
					}
				}
				/* simply check for graph and print
				 *	print <n> - <e> - one per line, sorty by from-node, then to-node
				 */
				case "print" -> {
					var name = parser.word(true).consume().expect("graph name");
					var container = graphs.get(name);
					if (container == null) {
						ErrorOutputBuilder.generic("Ich kenn keinen Graphen mit dem Namen " + name).write(invc).send();
						return;
					}
					actionPrintGraph(invc, container);
				}
				/* calculates max flow between v1 and v2
				 *	flow <n> <v1> <v2> - output is just <f>
				 */
				case "flow" -> {
					var name = parser.word(true).consume().expect("graph name");
					var container = graphs.get(name);
					if (container == null) {
						ErrorOutputBuilder.generic("Ich kenn keinen Graphen mit dem Namen " + name).write(invc).send();
						return;
					}
					actionMaxFlow(invc, container, parser);
				}
				// we obviously don't implement that, just remove all graphs
				case "quit" -> actionQuit(invc);
				default -> ErrorOutputBuilder.generic("Ich weiß leider nicht, was du von mir willst.").write(invc).send();
			}
		} catch (ChrislieParser.ParserException e) {
			ErrorOutputBuilder.generic(out -> out
					.append("Parserfehler\n")
					.append(e.getMessage(), ChrislieFormat.BLOCK)).write(invc).send();
		} catch (IllegalGraphException e) {
			ErrorOutputBuilder.generic(out -> out
					.append("Diese Operation würde einen ungültigen Graphen erzeugen.\n")
					.append(e.getMessage(), ChrislieFormat.BLOCK)).write(invc).send();
		}
	}
	
	private synchronized void actionAddNewGraph(Invocation invc, ChrislieParser parser, String name)
			throws ListenerException, ChrislieParser.ParserException, IllegalGraphException {
		if (graphs.size() >= LIMIT_MAX_GRAPH_COUNT) {
			ErrorOutputBuilder.generic("Ich hab jetzt %d Graphen gespeichert, das reicht!".formatted(graphs.size())).write(invc).send();
			return;
		}
		
		if (!GRAPH_NAME_PATTERN.matcher(name).matches()) {
			ErrorOutputBuilder.generic(out -> out.appendEscape("Der Name ").appendEscape(name, ChrislieFormat.BOLD).appendEscape(" ist ungültig."))
					.write(invc).send();
			return;
		}
		
		// parse graph before adding to graphs to maintain consistency
		var graph = FlowGraph.empty();
		
		var graphSpec = parser.word(true).consume().expect("graph definition");
		var edgeSpecs = graphSpec.split(";");
		if (edgeSpecs.length > LIMIT_MAX_EDGE_COUNT) {
			ErrorOutputBuilder.generic("Das sind %d Kanten, so viel will ich mir nicht merken!".formatted(edgeSpecs.length)).write(invc).send();
			return;
		}
		
		var edgeStore = new HashSet<Edge>();
		for (String edgeSpec : edgeSpecs) {
			// abort on error
			if (!applyGraphSpec(invc, graph, edgeSpec, edgeStore))
				return;
		}
		
		// ensure no trailing output
		parser.skipWhitespaces();
		if (parser.codepoint().canRead()) {
			ErrorOutputBuilder.generic("Da hängen Daten hinter der Graph Definition, die ich nicht verstehe.").write(invc).send();
			return;
		}
		
		graphs.put(name, new GraphContainer(name, graph));
		
		simpleOutput(warnInvalidGraph(graph, invc.reply()), "Graph `%s` angelegt".formatted(name), "Added new escape network with identifier %s.".formatted(name)).send();
	}
	
	private synchronized void actionModifyGraph(Invocation invc, ChrislieParser parser, GraphContainer container)
			throws ChrislieParser.ParserException, IllegalGraphException, ListenerException {
		var graph = container.graph;
		
		if (graph.nodeCount() > LIMIT_MAX_EDGE_COUNT) {
			ErrorOutputBuilder.generic("Mir egal was du vor hast, nicht mit diesem Graphen, der ist zu voll!").write(invc).send();
			return;
		}
		
		var edgeSpec = parser.word(true).consume().expect("edge spec");
		// ensure no trailing output
		parser.skipWhitespaces();
		if (parser.codepoint().canRead()) {
			ErrorOutputBuilder.generic("Da hängen Daten hinter der Graph Definition, die ich nicht verstehe.").write(invc).send();
			return;
		}
		
		// parser spec may confuse user if they intent to add new graph but name already exists, this is due to poor parser design but we can account for that
		if (edgeSpec.contains(";")) {
			ErrorOutputBuilder.generic("Mehrere Kanten kannst du nur bei neuen Graphen hinzufügen. Der Graph `%s` existiert jedoch schon.".formatted(container.name))
					.write(invc).send();
			return;
		}
		
		if (!applyGraphSpec(invc, graph, edgeSpec, new HashSet<>()))
			return;
		
		// clear max flow cache
		container.resultCache.clear();
		
		simpleOutput(warnInvalidGraph(graph, invc.reply()), "Graph `%s` aktualisiert".formatted(container.name),
				"Added new section %s to escape network %s.".formatted(edgeSpec, container.name)).send();
	}
	
	private synchronized void actionListFlows(Invocation invc, GraphContainer container) throws ListenerException {
		var reply = invc.reply();
		
		var list = new ArrayList<>(container.resultCache.entrySet());
		list.sort((o1, o2) -> {
			if (o1.getValue() < o2.getValue())
				return -1;
			if (o1.getValue() > o2.getValue())
				return 1;
			return o1.getKey().compareTo(o2.getKey());
		});
		
		if (list.isEmpty()) {
			simpleOutput(invc.reply(), "Keine Berechnungen für %s".formatted(container.name), "EMPTY").send();
		} else {
			var output = list.stream()
					.map(e -> "%d %s %s".formatted(e.getValue(), e.getKey().from().name(), e.getKey().to().name())).collect(Collectors.joining("\n"));
			simpleOutput(invc.reply(), "Verhandene Berechnungen für %s".formatted(container.name), output).send();
		}
	}
	
	private synchronized void actionListGraphs(Invocation invc) throws ListenerException {
		var list = new ArrayList<>(graphs.values());
		Collections.sort(list);
		
		if (list.isEmpty()) {
			simpleOutput(invc.reply(), "Keine Graphen vorhanden", "EMPTY").send();
		} else {
			var output = list.stream().map(c -> "%s %d".formatted(c.name, c.graph.nodeCount())).collect(Collectors.joining("\n"));
			simpleOutput(invc.reply(), "Vorhandene Graphen", output).send();
		}
	}
	
	private synchronized void actionPrintGraph(Invocation invc, GraphContainer container) throws ListenerException {
		var graph = container.graph;
		var list = new ArrayList<>(graph.toEdgeMap().entrySet());
		list.sort(Map.Entry.comparingByKey());
		var output = list.stream().map(e ->
				"%s%d%s".formatted(e.getKey().from().name(), e.getValue(), e.getKey().to().name())).collect(Collectors.joining("\n"));
		simpleOutput(invc.reply(), "Graphstruktur von %s".formatted(container.name), output).send();
	}
	
	private synchronized void actionMaxFlow(Invocation invc, GraphContainer container, ChrislieParser parser) throws ChrislieParser.ParserException, ListenerException {
		if (container.resultCache.size() > LIMIT_MAX_FLOW_CACHE) {
			ErrorOutputBuilder.generic("Wir haben genug maximale Flüsse berechnet, so viele brennbare " +
					"Gegenstände gibt es gar nicht, wie du Fluchtwege in diesem Graphen berechnen möchtest!!!").write(invc).send();
			return;
		}
		
		var graph = container.graph;
		var solver = MaxFlowSolver.ofFlowGraph(graph);
		
		var from = Node.withName(parser.word(true).consume().expect("start node"));
		if (!graph.contains(from)) {
			ErrorOutputBuilder.generic("Der Graph %s hat keinen Node mit dem Namen: %s".formatted(container.name, from.name())).write(invc).send();
			return;
		}
		
		var to = Node.withName(parser.word(true).consume().expect("finish node"));
		if (!graph.contains(to)) {
			ErrorOutputBuilder.generic("Der Graph %s hat keinen Node mit dem Namen: %s".formatted(container.name, to.name())).write(invc).send();
			return;
		}
		
		var maxFlow = container.resultCache.computeIfAbsent(FlowQuery.asQuery(from, to), query -> solver.maxFlow(FlowQuery.asQuery(from, to)));
		simpleOutput(invc.reply(), "Max Flow in `%s` von `%s` nach `%s` ist `%d`"
				.formatted(container.name, from.name(), to.name(), maxFlow), "%d".formatted(maxFlow)).send();
	}
	
	private ChrislieOutput warnInvalidGraph(FlowGraph graph, ChrislieOutput out) {
		var outOnly = new HashSet<>(graph.nodes());
		var inOnly = new HashSet<>(graph.nodes());
		
		for (var edge : graph.toEdgeMap().keySet()) {
			outOnly.remove(edge.to());
			inOnly.remove(edge.from());
		}
		
		if (outOnly.isEmpty() || inOnly.isEmpty()) {
			out.footer("Dieser Graph ist eigentlich ungültig, da er keinen Start- oder Zielknoten hat.");
		}
		
		return out;
	}
	
	private synchronized void actionQuit(Invocation invc) throws ListenerException {
		graphs.clear();
		simpleOutput(invc.reply(), "Ich habe aufgeräumt", ":)))").send();
	}
	
	private boolean applyGraphSpec(Invocation invc, FlowGraph graph, String spec, Set<Edge> edgeStore) throws ListenerException, IllegalGraphException {
		var matcher = GRAPH_NODE_SPEC_PATTERN.matcher(spec);
		
		if (!matcher.matches()) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Ungültige Kantenspezifikation: ").appendEscape(spec, ChrislieFormat.CODE))
					.write(invc).send();
			return false;
		}
		
		var from = Node.withName(matcher.group("from"));
		var to = Node.withName(matcher.group("to"));
		var edge = Edge.between(from, to);
		
		// check for duplicated edges
		if (!edgeStore.add(edge)) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Duplizierte Kante: ")
					.appendEscape(edge.toString(), ChrislieFormat.CODE))
					.write(invc).send();
			return false;
		}
		
		int capacity;
		try {
			capacity = Integer.parseInt(matcher.group("capacity"));
			if (capacity <= 0)
				throw new NumberFormatException("edge capacity must be greater zero");
		} catch (NumberFormatException e) {
			ErrorOutputBuilder.generic(out -> out
					.appendEscape("Ungültige Kapazität auf Kante: ")
					.appendEscape(edge.toString(), ChrislieFormat.CODE)
					.appendEscape(" mit Wert ")
					.appendEscape(matcher.group("capacity"), ChrislieFormat.CODE))
					.write(invc).send();
			return false;
		}
		
		graph.modifyEdge(edge, capacity);
		
		return true;
	}
	
	private ChrislieOutput simpleOutput(ChrislieOutput out, String title, String specOutput) {
		return out
				.thumbnail("https://chrisliebot.chrisliebaer.de/notausgang.png")
				.description(o -> o.append("\n" + specOutput, ChrislieFormat.BLOCK))
				.title(title);
	}
	
	@AllArgsConstructor
	private static final class GraphContainer implements Comparable<GraphContainer> {
		
		private final String name;
		private final FlowGraph graph;
		private final HashMap<FlowQuery, Long> resultCache = new HashMap<>();
		
		@Override
		public int compareTo(@NotNull KitEscapeRoutesCommand.GraphContainer o) {
			var intComp = Integer.compareUnsigned(o.graph.nodeCount(), graph.nodeCount());
			if (intComp != 0)
				return intComp;
			
			return name.compareTo(o.name);
		}
	}
}
