package dev.arcovia.mitigation.smt.preprocess;

import java.util.List;

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
		List<Label> relevantNodeLabelsAdd,
		List<Label> relevantNodeLabelsRemove,
		List<Label> relevantDataLabelsAdd,
		List<Label> relevantDataLabelsRemove,
		List<DFDVertexType> relevantNodeTypes
		) {}
