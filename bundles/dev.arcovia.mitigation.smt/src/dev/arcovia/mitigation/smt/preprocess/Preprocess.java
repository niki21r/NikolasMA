package dev.arcovia.mitigation.smt.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;

import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.actions.LabelAction;
import dev.arcovia.mitigation.smt.actions.LabelTypeAction;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Given an input DFD and constraints, performs preprocessing
 * 
 * @author Nikolas Rank
 *
 */
public class Preprocess {

	private final Logger logger = Logger.getLogger(getClass());

	/**
	 * Analyzes incoming constraints. If any reference labels that are not currently
	 * in the Datadictionary, they are added
	 * 
	 * @param dfd         Incoming DFD
	 * @param constraints Incoming Constraints
	 * @return DFD that possesses the required labels
	 */
	private DataFlowDiagramAndDictionary addMissingLabels(DataFlowDiagramAndDictionary dfd,
			List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> characteristicsSelectorData = Util.getAnalysisCharacteristics(constraints);

		DataDictionary dd = dfd.dataDictionary();

		for (CharacteristicsSelectorData data : characteristicsSelectorData) {
			if (!data.characteristicType().isConstant() || !data.characteristicValue().isConstant()) {
				logger.error("Variable detected in Constraints. Currently not supported. Exiting");
				System.exit(1);
			}
			String type = data.characteristicType().toString();
			if (!Util.containsLabelType(dd, type)) {
				LabelTypeAction modifyLabelType = new LabelTypeAction(type);
				modifyLabelType.doAction(dfd);
			}
			String value = data.characteristicValue().toString();
			LabelType parentType = Util.getLabelTypeByName(dd, type);
			if (!Util.containsLabel(parentType, value)) {
				LabelAction modifyLabel = new LabelAction(type, value);
				modifyLabel.doAction(dfd);
			}
		}
		return dfd;
	}

	/**
	 * Performs preprocessing on the incoming DFD. Adds missing labels. Extracts TFG
	 * Flows and DFDVertices from TFGs. Finds relevant node labels and types, as
	 * well as data labels based on constraints
	 * 
	 * @param dfdIn               Input DFD
	 * @param analysisConstraints Input constraints
	 * @return Record Type that has all relevant preprocessing information
	 */
	public PreprocessingResult preprocess(DataFlowDiagramAndDictionary dfdIn,
			List<AnalysisConstraint> analysisConstraints) {
		dfdIn = addMissingLabels(dfdIn, analysisConstraints);

		DataFlowDiagram dfd = dfdIn.dataFlowDiagram();
		DataDictionary dd = dfdIn.dataDictionary();

		DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(dd, dfd);
		DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
				.useCustomResourceProvider(dfdModelResourceProvider).build();
		DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
		List<DFDTransposeFlowGraph> tfgs = flowGraphs.getTransposeFlowGraphs().stream()
				.filter(DFDTransposeFlowGraph.class::isInstance).map(DFDTransposeFlowGraph.class::cast).toList();

		Map<Pin, List<AbstractAssignment>> outPinToAss = Util.outPinToAss(dfd.getNodes());

		List<DFDVertex> allVertices = new ArrayList<>();
		List<TFGFlow> allFlows = new ArrayList<TFGFlow>();
		// Create flows for each tfg
		for (int i = 0; i < tfgs.size(); i++) {
			DFDTransposeFlowGraph tfg = tfgs.get(i);
			List<DFDVertex> vertices = tfg.getVertices().stream().filter(DFDVertex.class::isInstance)
					.map(DFDVertex.class::cast).toList();
			Map<Pin, TFGFlow> outPinToTFGFlowMap = new HashMap<>();
			Map<TFGFlow, Pin> tfgFlowToInPinMap = new HashMap<>();
			List<TFGFlow> allTFGFlows = new ArrayList<>();
			Map<DFDVertex, List<TFGFlow>> allTFGFlowsToVertex = new HashMap<>();
			// Create flows for each dfdvertex
			for (int j = vertices.size() - 1; j >= 0; j--) {
				DFDVertex vertex = vertices.get(j);
				Map<Pin, DFDVertex> previousVerticesMap = vertex.getPinDFDVertexMap();
				Map<Pin, Flow> pinFlowMap = vertex.getPinFlowMap();
				// Create a flow for each incoming pin that is connected to a previous dfd
				// vertex
				for (Entry<Pin, Flow> pinFlow : pinFlowMap.entrySet()) {
					Flow flow = pinFlow.getValue();
					TFGFlow tfgFlow = new TFGFlow(flow.getSourcePin(),
							previousVerticesMap.get(flow.getDestinationPin()), flow.getDestinationPin(), vertex, flow);
					outPinToTFGFlowMap.put(flow.getSourcePin(), tfgFlow);
					tfgFlowToInPinMap.put(tfgFlow, flow.getDestinationPin());
					allTFGFlows.add(tfgFlow);
					List<TFGFlow> allTFGFlowsToThisVertex = allTFGFlowsToVertex.getOrDefault(vertex,
							new ArrayList<TFGFlow>());
					allTFGFlowsToThisVertex.add(tfgFlow);
					allTFGFlowsToVertex.put(vertex, allTFGFlowsToThisVertex);
				}
				// Create mappings, so we can later know which tfg flows are forwarded by which
				// and on which flows
				// assign statements have to be evaluated.
				List<TFGFlow> thisVertexOutgoingFlows = allTFGFlows.stream().filter(x -> x.srcVertex.equals(vertex))
						.toList();
				List<TFGFlow> allTFGFlowsToThisVertex = allTFGFlowsToVertex.getOrDefault(vertex,
						new ArrayList<TFGFlow>());
				for (TFGFlow tfgFlow : thisVertexOutgoingFlows) {
					List<AbstractAssignment> thisPinAssigns = outPinToAss.get(tfgFlow.srcPin);
					for (AbstractAssignment assign : thisPinAssigns) {
						if (assign instanceof ForwardingAssignment cast) {
							List<TFGFlow> thisFlowForwards = allTFGFlowsToThisVertex.stream()
									.filter(x -> cast.getInputPins().contains(x.dstPin)).toList();
							tfgFlow.thisFlowForwards.put(cast, thisFlowForwards);
						} else if (assign instanceof Assignment cast) {
							List<TFGFlow> thisFlowEvaluatesOn = allTFGFlowsToThisVertex.stream()
									.filter(x -> cast.getInputPins().contains(x.dstPin)).toList();
							tfgFlow.thisFlowEvaluatesOn.put(cast, thisFlowEvaluatesOn);
						}
					}
				}
				allVertices.add(vertex);
			}
			allFlows.addAll(allTFGFlows);
		}

		List<Label> relevantNodeLabelsAdd = Util.getRelevantNodeLabelsAdd(dd, analysisConstraints);
		List<Label> relevantNodeLabelsRemove = Util.getRelevantNodeLabelsRemove(dd, analysisConstraints);
		List<Label> relevantDataLabelsAdd = Util.getRelevantDataLabelsAdd(dd, analysisConstraints);
		List<Label> relevantDataLabelsRemove = Util.getRelevantDataLabelsRemove(dd, analysisConstraints);
		List<DFDVertexType> relevantNodeTypes = List.copyOf(Util.getRelevantVertexTypes(analysisConstraints));

		PreprocessingResult result = new PreprocessingResult(dfdIn, allFlows, allVertices, relevantNodeLabelsAdd,
				relevantNodeLabelsRemove, relevantDataLabelsAdd, relevantDataLabelsRemove, relevantNodeTypes);

		return result;
	}
}
