package dev.arcovia.mitigation.smt;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AND;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelReference;
import org.dataflowanalysis.dfd.datadictionary.NOT;
import org.dataflowanalysis.dfd.datadictionary.OR;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;
import org.dataflowanalysis.dfd.datadictionary.TRUE;
import org.dataflowanalysis.dfd.datadictionary.Term;
import org.dataflowanalysis.dfd.datadictionary.UnsetAssignment;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;

import dev.arcovia.mitigation.smt.actions.Action;
import dev.arcovia.mitigation.smt.actions.NodeLabelAddAction;
import dev.arcovia.mitigation.smt.actions.NodeLabelRemoveAction;
import dev.arcovia.mitigation.smt.actions.SetAssignmentAction;
import dev.arcovia.mitigation.smt.actions.UnsetAssignmentAction;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.constraints.DefaultSelectorTranslator;
import dev.arcovia.mitigation.smt.constraints.SelectorRole;
import dev.arcovia.mitigation.smt.constraints.SelectorTranslator;
import dev.arcovia.mitigation.smt.constraints.TranslationEnv;
import dev.arcovia.mitigation.smt.cost.CostFunction;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.util.SMTUtil;
import dev.arcovia.mitigation.smt.util.Util;

public class SMT {
	private Context ctx;
	private Optimize opt;
	private PreprocessingResult pre;
	private SMTMappings mappings;
	private IntExpr costFunction;
	private Config config;
	public Map<TFGFlow, IntNum> flowNames;
	public Map<Node, IntNum> nodeNames;
	public Map<DFDVertexType, IntNum> nodeTypes;
	private List<AnalysisConstraint> constraints;
	Map<Node, Map<Label, BoolExpr>> nodeLabelRef = new HashMap<>();
	Map<Node, Map<Label, BoolExpr>> nodeLabels = new HashMap<>();
	public Map<TFGFlow, Map<Label, BoolExpr>> flowLabels;
	Map<Pin, Map<Label, BoolExpr>> pinSet;
	Map<Pin, Map<Label, BoolExpr>> pinUnset;
	public Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows;
	public Map<Pin, List<AbstractAssignment>> outPinToAss;

	public SMT(PreprocessingResult pre, List<AnalysisConstraint> constraints, SMTMappings mappings,
			Map<String, Integer> labelCosts, Config config) {
		this.config = config;
		this.constraints = constraints;
		this.ctx = new Context();
		this.opt = ctx.mkOptimize();
		this.pre = pre;
		this.mappings = mappings;
		vertexIncomingFlows = new HashMap<>();
		for (TFGFlow flow : pre.flows()) {
			List<TFGFlow> flows = vertexIncomingFlows.getOrDefault(flow.dstVertex, new ArrayList<>());
			flows.add(flow);
			vertexIncomingFlows.put(flow.dstVertex, flows);
		}
		flowNames = new HashMap<TFGFlow, IntNum>();
		nodeNames = new HashMap<Node, IntNum>();
		nodeTypes = new EnumMap<DFDVertexType, IntNum>(DFDVertexType.class);
		nodeLabelRef = new HashMap<>();
		nodeLabels = new HashMap<>();
		flowLabels = new HashMap<>();
		pinSet = new HashMap<>();
		pinUnset = new HashMap<>();
		outPinToAss = Util.outPinToAss(pre.dfd().dataFlowDiagram().getNodes());

		initializeStructure();
		CostFunction costFunctionBuilder = CostFunction.create(ctx);
		Map<Label, Integer> labelWeights = (labelCosts == null) ? Map.of()
				: Util.transformLabelCosts(pre.dfd().dataDictionary(), labelCosts);
		for (Entry<Node, Map<Label, BoolExpr>> map : nodeLabelRef.entrySet()) {
			// Get node cost here			
			int nodeCost = 1;
			/*
			int nodeCost = 0;
			for (DFDVertex vertex: pre.vertices()) {
				if (vertex.getReferencedElement().equals(map.getKey())) {
					nodeCost++;
				}
			}*/
			for (Entry<Label, BoolExpr> ref : map.getValue().entrySet()) {
				costFunctionBuilder.add(nodeLabels.get(map.getKey()).get(ref.getKey()), ref.getValue(),
						labelWeights.getOrDefault(ref.getKey(), 1) * nodeCost);
			}
		}
		for (Entry<Pin, Map<Label, BoolExpr>> map : pinSet.entrySet()) {
			// Get pincost here
			int pinCost = 1;
			for (Entry<Label, BoolExpr> set : map.getValue().entrySet()) {
				costFunctionBuilder.add(set.getValue(), ctx.mkFalse(),
						labelWeights.getOrDefault(set.getKey(), 1) * pinCost);
			}
		}
		for (Entry<Pin, Map<Label, BoolExpr>> map : pinUnset.entrySet()) {
			int pinCost = 1;
			for (Entry<Label, BoolExpr> unset : map.getValue().entrySet()) {
				costFunctionBuilder.add(unset.getValue(), ctx.mkFalse(),
						labelWeights.getOrDefault(unset.getKey(), 1) * pinCost);
			}
		}

		costFunction = costFunctionBuilder.build();
		/*
		System.out.println(costFunction.toString());
		*/
		createDataFlowConstraints();
		createUserConstraints(constraints);
		opt.MkMinimize(costFunction);
	}

	public SolvingResult repair() {
		long before = System.currentTimeMillis();
		Status st = opt.Check();
		long after = System.currentTimeMillis();
		long solveTime = after - before;
		if (st != Status.SATISFIABLE) {
			ctx.close();
			return new SolvingResult(false, null, null, Integer.MAX_VALUE);
		} else {
			Model m = opt.getModel();
			IntExpr costValExpr = (IntExpr) m.eval(costFunction, true);
			List<Action> parseActions = parseActions(m);
			DataFlowDiagramAndDictionary dfd = pre.dfd();
			for (int i = 0; i < parseActions.size(); i++) {
				dfd = parseActions.get(i).doAction(dfd);
			}
			return new SolvingResult(true, dfd, parseActions, Integer.parseInt(costValExpr.toString()));
		}
	}

	public List<Action> suggestActions() {
		Status st = opt.Check();
		Model m = opt.getModel();
		IntExpr costVal = (IntExpr) m.eval(costFunction, true);
		List<Action> parseActions = parseActions(m);
		System.out.println("Cost " + costVal.toString());
		System.out.println("Actions " + parseActions);
		/*
		 * if (Integer.parseInt(costVal.toString()) != parseActions.size()) {
		 * System.out.println("Mismatch found"); System.exit(1); }
		 */
		ctx.close();
		return parseActions;
	}

	public long getDagSizeAfterSolving() {
		Status st = opt.Check();
		Model m = opt.getModel();
		IntExpr costVal = (IntExpr) m.eval(costFunction, true);
		List<Action> parseActions = parseActions(m);
		DataFlowDiagramAndDictionary dfd = pre.dfd();
		for (int i = 0; i < parseActions.size(); i++) {
			dfd = parseActions.get(i).doAction(dfd);
		}
		BoolExpr[] assertions = opt.getAssertions();
		long astNodes = SMTUtil.countAstNodes(assertions);
		ctx.close();
		return astNodes;
	}

	private void createUserConstraints(List<AnalysisConstraint> constraints) {
		TranslationEnv env = new TranslationEnv(ctx, opt, pre, mappings, vertexIncomingFlows, flowLabels, flowNames,
				nodeLabels);
		SelectorTranslator translator = new DefaultSelectorTranslator(env);
		for (AnalysisConstraint constr : constraints) {
			List<AbstractSelector> dataSource = constr.getDataSourceSelectors().getSelectors();
			List<AbstractSelector> vertexSource = constr.getVertexSourceSelectors().getSelectors();
			List<AbstractSelector> vertexDestination = constr.getVertexDestinationSelectors().getSelectors();

			for (DFDVertex vertex : pre.vertices()) {

				List<BoolExpr> allDestinationSelectors = new ArrayList<>();
				for (AbstractSelector dstSelector : vertexDestination) {
					allDestinationSelectors
							.add(translator.toBool(dstSelector, vertex, SelectorRole.VERTEX_DESTINATION));
				}
				BoolExpr allDestinationSatisfied = ctx.mkAnd(allDestinationSelectors.toArray(new BoolExpr[0]));

				List<BoolExpr> allDataSource = new ArrayList<>();
				for (AbstractSelector source : dataSource) {
					allDataSource.add(translator.toBool(source, vertex, SelectorRole.DATA_SOURCE));
				}
				BoolExpr allDataSourceSatisfied = ctx.mkAnd(allDataSource.toArray(new BoolExpr[0]));

				List<BoolExpr> allVertexSource = new ArrayList<>();
				for (AbstractSelector source : vertexSource) {
					allVertexSource.add(translator.toBool(source, vertex, SelectorRole.VERTEX_SOURCE));
				}
				BoolExpr allVertexSourceSatisfied = ctx.mkAnd(allVertexSource.toArray(new BoolExpr[0]));

				BoolExpr allSatisfied = ctx.mkAnd(allDestinationSatisfied, allDataSourceSatisfied,
						allVertexSourceSatisfied);
				BoolExpr notAllSatisfied = ctx.mkNot(allSatisfied);
				opt.Assert(new BoolExpr[] { notAllSatisfied });
			}
		}
	}

	private void createDataFlowConstraint(TFGFlow flow) {
		if (flowLabels.get(flow) != null) {
			return;
		} else {
			flow.thisFlowForwards.values().forEach(x -> x.forEach(y -> createDataFlowConstraint(y)));
			flow.thisFlowEvaluatesOn.values().forEach(x -> x.forEach(y -> createDataFlowConstraint(y)));
			flowLabels.put(flow, new HashMap<>());
			Pin pin = flow.srcPin;
			List<AbstractAssignment> assignments = outPinToAss.get(pin);
			List<Label> allDataLabels = new ArrayList<>();
			allDataLabels.addAll(pre.relevantDataLabelsAdd());
			allDataLabels.addAll(pre.relevantDataLabelsRemove());
			for (Label label : allDataLabels) {
				BoolExpr labelExpr = ctx.mkFalse();
				for (int i = 0; i < assignments.size(); i++) {
					AbstractAssignment assignment = assignments.get(i);
					if (assignment instanceof SetAssignment cast && cast.getOutputLabels().contains(label)) {
						labelExpr = ctx.mkTrue();
					} else if (assignment instanceof UnsetAssignment cast && cast.getOutputLabels().contains(label)) {
						labelExpr = ctx.mkFalse();
					} else if (assignment instanceof ForwardingAssignment cast) {
						List<TFGFlow> forward = flow.thisFlowForwards.getOrDefault(cast, new ArrayList<>());
						for (TFGFlow pre : forward) {
							BoolExpr preLabel = flowLabels.get(pre).get(label);
							labelExpr = ctx.mkOr(labelExpr, preLabel);
						}
					} else if (assignment instanceof Assignment cast && cast.getOutputLabels().contains(label)) {
						List<TFGFlow> evaluateOn = flow.thisFlowEvaluatesOn.getOrDefault(cast, new ArrayList<>());
						labelExpr = createTerm(cast.getTerm(), evaluateOn);
					}
				}
				BoolExpr pinNewSet = pinSet.get(pin).get(label);
				if (pinNewSet != null) {
					labelExpr = ctx.mkOr(labelExpr, pinNewSet);
				}
				BoolExpr pinNewUnset = pinUnset.get(pin).get(label);
				if (pinNewUnset != null) {
					labelExpr = ctx.mkAnd(labelExpr, ctx.mkNot(pinNewUnset));
				}
				flowLabels.get(flow).put(label, labelExpr);
			}
		}
	}

	private void createDataFlowConstraints() {
		List<TFGFlow> allFlows = pre.flows();
		if ((!pre.relevantDataLabelsAdd().isEmpty() || !pre.relevantDataLabelsRemove().isEmpty())) {
			for (TFGFlow flow : allFlows) {
				createDataFlowConstraint(flow);
			}
		}
	}

	private BoolExpr createTerm(Term term, List<TFGFlow> evaluateOn) {
		if (term instanceof TRUE) {
			return ctx.mkTrue();
		} else if (term instanceof NOT cast) {
			return ctx.mkNot(createTerm(cast.getNegatedTerm(), evaluateOn));
		} else if (term instanceof AND cast) {
			List<Term> subTerms = cast.getTerms();
			List<BoolExpr> subExprs = subTerms.stream().map(x -> createTerm(x, evaluateOn)).toList();
			return ctx.mkAnd(subExprs.toArray(new BoolExpr[0]));
		} else if (term instanceof OR cast) {
			List<Term> subTerms = cast.getTerms();
			List<BoolExpr> subExprs = subTerms.stream().map(x -> createTerm(x, evaluateOn)).toList();
			return ctx.mkOr(subExprs.toArray(new BoolExpr[0]));
		} else if (term instanceof LabelReference cast) {
			Label label = cast.getLabel();
			List<BoolExpr> incomingMatches = new ArrayList<>();
			for (TFGFlow f : evaluateOn) {
				BoolExpr evaluateLabel = flowLabels.get(f).get(label);
				incomingMatches.add(evaluateLabel);
			}
			return ctx.mkOr(incomingMatches.toArray(new BoolExpr[0]));
		} else {
			throw new IllegalArgumentException("Unknown term: " + term);
		}
	}

	public void initializeStructure() {
		initializePins();
		initializeFlows();
		initializeNodes();
	}

	private void initializePins() {
		List<Pin> allOutPins = pre.dfd().dataDictionary().getBehavior().stream().flatMap(x -> x.getOutPin().stream())
				.toList();

		List<Label> dataLabelsAdd = config.addDataLabels() ? pre.relevantDataLabelsAdd() : new ArrayList<>();
		List<Label> dataLabelsRemove = config.removeDataLabels() ? pre.relevantDataLabelsRemove() : new ArrayList<>();

		if (config.onlyRelevantLabels()) {
			for (Pin pin : allOutPins) {
				Map<Label, BoolExpr> set = new HashMap<>();

				for (Label label : dataLabelsAdd) {
					set.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
				}
				pinSet.put(pin, set);
			}
			for (Pin pin : allOutPins) {
				Map<Label, BoolExpr> unset = new HashMap<>();

				for (Label label : dataLabelsRemove) {
					unset.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
				}
				pinUnset.put(pin, unset);
			}
		} else {
			List<Label> allDataLabels = new ArrayList<>();
			allDataLabels.addAll(dataLabelsRemove);
			allDataLabels.addAll(dataLabelsAdd);
			for (Pin pin : allOutPins) {
				Map<Label, BoolExpr> set = new HashMap<>();
				Map<Label, BoolExpr> unset = new HashMap<>();

				for (Label label : allDataLabels) {
					set.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_set_" + label.getEntityName()));
					unset.put(label, ctx.mkBoolConst("Pin_" + pin.getId() + "_unset_" + label.getEntityName()));
				}
				pinSet.put(pin, set);
				pinUnset.put(pin, unset);
			}
		}
	}

	private void initializeFlows() {
		if (Util.containsFlowNameSelector(constraints)) {
			Map<String, Integer> flowNameMap = mappings.flowNameToInt;
			for (TFGFlow flow : pre.flows()) {
				IntNum num = ctx.mkInt(flowNameMap.get(flow.flow.getEntityName()));
				flowNames.put(flow, num);
			}
		}
	}

	private void initializeNodes() {
		// Create node name ints
		if (Util.containsVertexNameSelector(constraints)) {
			Map<String, Integer> nodeNameMap = mappings.nodeNameToInt;
			for (Node node : pre.dfd().dataFlowDiagram().getNodes()) {
				IntNum name = ctx.mkInt(nodeNameMap.get(node.getEntityName()));
				nodeNames.put(node, name);
			}
		}
		if (Util.containsVertexTypeSelector(constraints)) {
			// Create type ints
			Map<DFDVertexType, Integer> nodeTypesPre = mappings.vertexTypeToInt;
			for (Entry<DFDVertexType, Integer> entry : nodeTypesPre.entrySet()) {
				IntNum type = ctx.mkInt(entry.getValue());
				nodeTypes.put(entry.getKey(), type);
			}
		}
		List<Label> nodeLabelsAdd = config.addNodeLabels() ? pre.relevantNodeLabelsAdd() : new ArrayList<>();
		List<Label> nodeLabelsRemove = config.removeNodeLabels() ? pre.relevantNodeLabelsRemove() : new ArrayList<>();
		Set<Label> allNodeLabels = new HashSet<>();
		allNodeLabels.addAll(pre.relevantNodeLabelsAdd());
		allNodeLabels.addAll(pre.relevantNodeLabelsRemove());
		if (config.onlyRelevantLabels()) {
			for (Node node : pre.dfd().dataFlowDiagram().getNodes()) {
				Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());
				Map<Label, BoolExpr> thisNodeLabelRef = new HashMap<>();
				Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();
				for (Label label : allNodeLabels) {
					// If label can be added or removed
					if (nodeLabelsAdd.contains(label) && nodeLabelsRemove.contains(label)) {
						thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
						thisNodeLabelVar.put(label,
								ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
					}
					// If label can only be added, only create it for nodes that do not posses the
					// label
					else if (nodeLabelsAdd.contains(label) && !thisNodeLabels.contains(label)) {
						thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());

						thisNodeLabelVar.put(label,
								ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
					}
					// if label can only be removed, only create it for nodes that possess the label
					else if (nodeLabelsRemove.contains(label) && thisNodeLabels.contains(label)) {
						thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
						thisNodeLabelVar.put(label,
								ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
					} else {
						thisNodeLabelVar.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());
					}
				}
				nodeLabelRef.put(node, thisNodeLabelRef);
				nodeLabels.put(node, thisNodeLabelVar);
			}
		} else {
			if (!allNodeLabels.isEmpty()) {
				for (Node node : pre.dfd().dataFlowDiagram().getNodes()) {
					Set<Label> thisNodeLabels = new HashSet<>(node.getProperties());

					Map<Label, BoolExpr> thisNodeLabelRef = new HashMap<>();
					Map<Label, BoolExpr> thisNodeLabelVar = new HashMap<>();

					for (Label label : allNodeLabels) {
						thisNodeLabelRef.put(label, thisNodeLabels.contains(label) ? ctx.mkTrue() : ctx.mkFalse());

						thisNodeLabelVar.put(label,
								ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName()));
					}

					nodeLabelRef.put(node, thisNodeLabelRef);
					nodeLabels.put(node, thisNodeLabelVar);
				}
			}

		}
	}

	private List<Action> parseActions(Model m) {
		List<Action> changes = new ArrayList<>();

		for (Node n : nodeLabelRef.keySet()) {
			Map<Label, BoolExpr> beforeMap = nodeLabelRef.get(n);
			Map<Label, BoolExpr> afterMap = nodeLabels.get(n);

			for (Label lbl : beforeMap.keySet()) {
				BoolExpr beforeExpr = beforeMap.get(lbl);
				BoolExpr afterExpr = afterMap.get(lbl);

				boolean beforeVal = ((BoolExpr) m.evaluate(beforeExpr, true)).isTrue();
				boolean afterVal = ((BoolExpr) m.evaluate(afterExpr, true)).isTrue();

				if (!beforeVal && afterVal) {
					changes.add(new NodeLabelAddAction(n, lbl));
				} else if (beforeVal && !afterVal) {
					changes.add(new NodeLabelRemoveAction(n, lbl));
				}
			}
		}
		for (Pin p : pinSet.keySet()) {
			Map<Label, BoolExpr> setMap = pinSet.get(p);

			for (Label label : setMap.keySet()) {
				BoolExpr setExpr = setMap != null ? setMap.get(label) : null;
				if (setExpr != null && ((BoolExpr) m.evaluate(setExpr, true)).isTrue()) {
					changes.add(new SetAssignmentAction(p, label));
				}
			}
		}
		for (Pin p : pinUnset.keySet()) {
			Map<Label, BoolExpr> unsetMap = pinUnset.get(p);
			for (Label label : unsetMap.keySet()) {
				BoolExpr unsetExpr = unsetMap != null ? unsetMap.get(label) : null;

				if (unsetExpr != null && ((BoolExpr) m.evaluate(unsetExpr, true)).isTrue()) {
					changes.add(new UnsetAssignmentAction(p, label));
				}
			}

		}

		return changes;
	}
}
