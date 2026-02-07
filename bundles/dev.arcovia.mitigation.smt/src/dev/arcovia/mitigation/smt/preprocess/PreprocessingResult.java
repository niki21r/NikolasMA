package dev.arcovia.mitigation.smt.preprocess;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;

import dev.arcovia.mitigation.smt.TFGFlow;

/**
 * @author Nikolas Rank
 * Record type that possesses all relevant preprocessing information.
 */
public record PreprocessingResult(
		DataFlowDiagramAndDictionary dfd,
		List<TFGFlow> flows,
		List<DFDVertex> vertices,
		Set<Label> relevantNodeLabelsAdd,
		Set<Label> relevantNodeLabelsRemove,
		Set<Label> relevantDataLabelsAdd,
		Set<Label> relevantDataLabelsRemove,
		List<DFDVertexType> relevantNodeTypes,
		Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows
		) {}
