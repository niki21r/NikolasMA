package dev.arcovia.mitigation.smt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.CharacteristicsSelectorData;
import org.dataflowanalysis.analysis.dsl.selectors.ConditionalSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.External;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * @author Nikolas Rank Contains parsing functions
 */
public class Util {

	private static final Logger logger = Logger.getLogger(Util.class);

	/**
	 * Maps characteristisc to Labels, given a contextual datadictionary
	 * 
	 * @param dd    Datadictionary that contains labels
	 * @param chars Incoming Characteristics
	 * @return List of labels encoded in characteristics
	 */
	public static List<Label> getLabelsForCharacteristics(DataDictionary dd, List<CharacteristicsSelectorData> chars) {
		List<Label> result = new ArrayList<>();
		for (CharacteristicsSelectorData data : chars) {
			LabelType labelType = getLabelTypeByName(dd, data.characteristicType().toString());
			Label label = getLabelByName(labelType, data.characteristicValue().toString());
			result.add(label);
		}
		return result;
	}

	public static DFDVertexType vertexToType(DFDVertex vertex) {
		Node n = vertex.getReferencedElement();
		if (n instanceof External) {
			return DFDVertexType.EXTERNAL;
		} else if (n instanceof Process) {
			return DFDVertexType.PROCESS;
		} else {
			return DFDVertexType.STORE;
		}
	}

	/**
	 * Finds relevant vertex types, based on incoming constraints
	 * 
	 * @param constraints Incoming constraints
	 * @return Set of relevant vertex types
	 */
	public static Set<DFDVertexType> getRelevantVertexTypes(List<AnalysisConstraint> constraints) {
		Set<DFDVertexType> result = new HashSet<>();
		for (AnalysisConstraint constr : constraints) {
			List<AbstractSelector> allSelectors = constr.getVertexDestinationSelectors().getSelectors();
			allSelectors.addAll(constr.getVertexSourceSelectors().getSelectors());
			for (AbstractSelector selector : allSelectors) {
				if (selector instanceof VertexTypeSelector cast) {
					DFDVertexType type = (DFDVertexType) cast.getVertexType();
					result.add(type);
				}
			}
		}
		return result;
	}

	/**
	 * Finds relevant Node labels of a datadictionary, i.e. those appearing in
	 * vertex constraints
	 * 
	 * @param dd          Datadictionary
	 * @param constraints constraints
	 * @return List of relevant node labels
	 */
	public static List<Label> getRelevantNodeLabelsAdd(DataDictionary dd, List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> relevantNodeCharacteristics = getAnalysisNodeCharacteristics(constraints,
				true);
		return getLabelsForCharacteristics(dd, relevantNodeCharacteristics);
	}

	/**
	 * Finds relevant Node labels of a datadictionary, i.e. those appearing in
	 * vertex constraints
	 * 
	 * @param dd          Datadictionary
	 * @param constraints constraints
	 * @return List of relevant node labels
	 */
	public static List<Label> getRelevantNodeLabelsRemove(DataDictionary dd, List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> relevantNodeCharacteristics = getAnalysisNodeCharacteristics(constraints,
				false);
		return getLabelsForCharacteristics(dd, relevantNodeCharacteristics);
	}

	/**
	 * Finds relevant data labels of a Datadictionary, i.e. those appearing in data
	 * constraints
	 * 
	 * @param dd          Datadictionary
	 * @param constraints constraints
	 * @return List of relevant Data labels
	 */
	public static List<Label> getRelevantDataLabelsAdd(DataDictionary dd, List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> relevantDataCharacteristics = getAnalysisDataCharacteristics(constraints,
				true);
		return getLabelsForCharacteristics(dd, relevantDataCharacteristics);
	}

	/**
	 * Finds relevant data labels of a Datadictionary, i.e. those appearing in data
	 * constraints
	 * 
	 * @param dd          Datadictionary
	 * @param constraints constraints
	 * @return List of relevant Data labels
	 */
	public static List<Label> getRelevantDataLabelsRemove(DataDictionary dd, List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> relevantDataCharacteristics = getAnalysisDataCharacteristics(constraints,
				false);
		return getLabelsForCharacteristics(dd, relevantDataCharacteristics);
	}

	/**
	 * Given a list of analysis constraints, extracts vertex characteristics
	 * 
	 * @param constraints Incoming constraints
	 * @return List of vertex characteristics in constraints
	 */
	public static List<CharacteristicsSelectorData> getAnalysisNodeCharacteristics(List<AnalysisConstraint> constraints,
			boolean add) {
		List<CharacteristicsSelectorData> characteristicsSelectorData = new ArrayList<>();
		for (AnalysisConstraint constr : constraints) {
			List<AbstractSelector> allSelectors = constr.getVertexDestinationSelectors().getSelectors();
			allSelectors.addAll(constr.getVertexSourceSelectors().getSelectors());
			for (AbstractSelector selector : allSelectors) {
				if (selector instanceof VertexCharacteristicsListSelector cast && (cast.isInverted() == add)) {
					characteristicsSelectorData.addAll(cast.getVertexCharacteristics());
				} else if (selector instanceof VertexCharacteristicsSelector cast && (cast.isInverted() == add)) {
					characteristicsSelectorData.add(cast.getVertexCharacteristics());
				} else if (selector instanceof ConditionalSelector) {
					logger.error("Conditional Selector detected. Currently not supported. Exiting");
					System.exit(1);
				}
			}
		}
		return characteristicsSelectorData;
	}

	/**
	 * Given a list of analysis constraints, extracts data characteristics
	 * 
	 * @param constraints Incoming constraints
	 * @return List of data characteristics in constraints
	 */
	public static List<CharacteristicsSelectorData> getAnalysisDataCharacteristics(List<AnalysisConstraint> constraints,
			boolean add) {
		List<CharacteristicsSelectorData> characteristicsSelectorData = new ArrayList<>();

		for (AnalysisConstraint constr : constraints) {
			List<AbstractSelector> allSelectors = constr.getDataSourceSelectors().getSelectors();
			for (AbstractSelector selector : allSelectors) {
				if (selector instanceof DataCharacteristicsSelector cast && (cast.isInverted() == add)) {
					characteristicsSelectorData.add(cast.getDataCharacteristic());
				} else if (selector instanceof DataCharacteristicListSelector cast && (cast.isInverted() == add)) {
					characteristicsSelectorData.addAll(cast.getDataCharacteristics());
				} else if (selector instanceof ConditionalSelector) {
					logger.error("Conditional Selector detected. Currently not supported. Exiting");
					System.exit(1);
				}
			}
		}
		return characteristicsSelectorData;
	}

	public static boolean containsFlowNameSelector(List<AnalysisConstraint> constraints) {
		return constraints.stream().map(x -> x.getDataSourceSelectors().getSelectors())
				.anyMatch(VariableNameSelector.class::isInstance);
	}

	public static boolean containsVertexNameSelector(List<AnalysisConstraint> constraints) {
		return constraints.stream().map(x -> x.getVertexDestinationSelectors().getSelectors())
				.anyMatch(VertexNameSelector.class::isInstance);
	}

	public static boolean containsVertexTypeSelector(List<AnalysisConstraint> constraints) {
		return constraints.stream()
				.flatMap(c -> Stream.concat(c.getVertexDestinationSelectors().getSelectors().stream(),
						c.getVertexSourceSelectors().getSelectors().stream()))
				.anyMatch(VertexTypeSelector.class::isInstance);
	}

	/**
	 * Extracts all data/vertex characteristics from constraints
	 * 
	 * @param constraints List of constraints
	 * @return List of characteristics
	 */
	public static List<CharacteristicsSelectorData> getAnalysisCharacteristics(List<AnalysisConstraint> constraints) {
		List<CharacteristicsSelectorData> all = new ArrayList<>();
		all.addAll(getAnalysisDataCharacteristics(constraints, false));
		all.addAll(getAnalysisDataCharacteristics(constraints, true));
		all.addAll(getAnalysisNodeCharacteristics(constraints, false));
		all.addAll(getAnalysisNodeCharacteristics(constraints, true));
		return all;
	}

	public static Map<Label, Integer> transformLabelCosts(DataDictionary dd, Map<String, Integer> labelCosts) {
		Map<Label, Integer> result = new HashMap<>();
		labelCosts.forEach((string, integer) -> result
				.put(getLabelByName(dd, string.split("\\.")[0], string.split("\\.")[1]), integer));
		return result;
	}

	/**
	 * Finds label types by name
	 * 
	 * @param dd   Datadictionary
	 * @param name Label Type name
	 * @return Label type, if exists, else crashe
	 */
	public static LabelType getLabelTypeByName(DataDictionary dd, String name) {
		return dd.getLabelTypes().stream().filter(x -> x.getEntityName().equals(name)).findFirst().get();
	}

	/**
	 * Finds label by name
	 * 
	 * @param labelType Type of the label
	 * @param name      Name of the label
	 * @return Label, if exists, else crashes
	 */
	public static Label getLabelByName(LabelType labelType, String name) {
		return labelType.getLabel().stream().filter(x -> x.getEntityName().equals(name)).findFirst().get();
	}

	public static Label getLabelByName(DataDictionary dd, String typeName, String labelName) {
		LabelType labelType = getLabelTypeByName(dd, typeName);
		return getLabelByName(labelType, labelName);
	}

	/**
	 * Checks if label type with name is present in Datadictionary
	 * 
	 * @param dd   Data dictionary
	 * @param name of the label type
	 * @return True if present, else false
	 */
	public static boolean containsLabelType(DataDictionary dd, String name) {
		return dd.getLabelTypes().stream().anyMatch(x -> x.getEntityName().equals(name));
	}

	/**
	 * Checks if label type contains a label with name
	 * 
	 * @param type Present label type
	 * @param name Name of label
	 * @return True if label type contains label else false
	 */
	public static boolean containsLabel(LabelType type, String name) {
		return type.getLabel().stream().anyMatch(x -> x.getEntityName().equals(name));
	}

	/**
	 * Given a list of nodes, maps output pins to their respective assignments
	 * 
	 * @param nodes List of nodes
	 * @return Map
	 */
	public static Map<Pin, List<AbstractAssignment>> outPinToAss(List<Node> nodes) {
		Map<Pin, List<AbstractAssignment>> outPinToAss = new HashMap<Pin, List<AbstractAssignment>>();
		for (Node n : nodes) {
			Behavior b = n.getBehavior();
			for (int i = 0; i < b.getAssignment().size(); i++) {
				AbstractAssignment a = b.getAssignment().get(i);
				List<AbstractAssignment> list = outPinToAss.getOrDefault(a.getOutputPin(),
						new ArrayList<AbstractAssignment>());
				list.add(a);
				outPinToAss.put(a.getOutputPin(), list);
			}
		}
		return outPinToAss;
	}

	/**
	 * Finds pin by id
	 * 
	 * @param id   id of the pin
	 * @param pins List of pins to search
	 * @return Pin if exists, else crashes
	 */
	public static Pin getPinById(String id, List<Pin> pins) {
		return pins.stream().filter(x -> x.getId().equals(id)).findFirst().get();
	}

	/**
	 * Counts violations of a DFD using existing tooling
	 * 
	 * @param dfd                 Input dfd
	 * @param analysisConstraints Constraints to evaluate on
	 * @return Amount of violations
	 */
	public static int countViolations(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> analysisConstraints) {
		DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(),
				dfd.dataFlowDiagram());
		DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
				.useCustomResourceProvider(dfdModelResourceProvider).build();
		DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
		flowGraphs.evaluate();

		List<DSLResult> result = new ArrayList<>();
		for (int i = 0; i < analysisConstraints.size(); i++) {
			List<DSLResult> violations = analysisConstraints.get(i).findViolations(flowGraphs);
			result.addAll(violations);
		}
		if (result.size() > 0) {
			System.out.println("DFA found " + result.size() + " tfg violations");
			return result.size();
		} else {
			// System.out.println("No confidentiality violations found.");
			return 0;
		}

	}

}
