package dev.arcovia.mitigation.smt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;

/**
 * @author Nikolas Rank Computes mappings from DFD attributes to integers.
 *
 */
public final class SMTMappings {

	PreprocessingResult pre;
	// Mapping of Node names to integers
	public final Map<String, Integer> nodeNameToInt;
	// Mapping of flow names to integers
	public final Map<String, Integer> flowNameToInt;
	// Mapping of vertex types to Integers
	public final Map<DFDVertexType, Integer> vertexTypeToInt;

	/**
	 * Takesk preprocessing result and constructs unmodifiable mappings that can be accessed 
	 * @param pre Preprocessing Result
	 */
	public SMTMappings(PreprocessingResult pre) {
		this.pre = pre;
		DataFlowDiagramAndDictionary dfd = pre.dfd();
		List<Node> nodes = dfd.dataFlowDiagram().getNodes();
		List<Flow> flows = dfd.dataFlowDiagram().getFlows();
		List<DFDVertexType> types = pre.relevantNodeTypes();
		nodeNameToInt = Map.copyOf(createIndexMap(nodes, Node::getEntityName));
		flowNameToInt = Map.copyOf(createIndexMap(flows, Flow::getEntityName));
		vertexTypeToInt = Map.copyOf(createIndexMap(types, Function.identity()));
	}

	/**
	 * Creates a map with increasing values
	 * @param <T> Type of Input items
	 * @param <K> Type of Keys of the map
	 * @param items List of input items
	 * @param keyExtractor Function that derives a key from input items
	 * @return Maps keys to integers
	 */
	private <T, K> Map<K, Integer> createIndexMap(List<T> items, Function<T, K> keyExtractor) {
		Map<K, Integer> map = new HashMap<>();
		int count = 0;
		for (T item : items) {
			if (map.containsKey(item)) continue;
			map.putIfAbsent(keyExtractor.apply(item), count++);
		}
		return map;
	}

}
