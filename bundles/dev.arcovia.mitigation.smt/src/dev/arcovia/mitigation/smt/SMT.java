package dev.arcovia.mitigation.smt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
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
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;

import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.CostConfig;
import dev.arcovia.mitigation.smt.constraints.DefaultSelectorTranslator;
import dev.arcovia.mitigation.smt.constraints.SelectorRole;
import dev.arcovia.mitigation.smt.constraints.SelectorTranslator;
import dev.arcovia.mitigation.smt.constraints.TranslationEnv;
import dev.arcovia.mitigation.smt.cost.CostFunction;
import dev.arcovia.mitigation.smt.operations.NodeLabelAddOperation;
import dev.arcovia.mitigation.smt.operations.NodeLabelRemoveOperation;
import dev.arcovia.mitigation.smt.operations.Operation;
import dev.arcovia.mitigation.smt.operations.SetAssignmentOperation;
import dev.arcovia.mitigation.smt.operations.UnsetAssignmentOperation;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import dev.arcovia.mitigation.smt.util.SMTUtil;
import dev.arcovia.mitigation.smt.util.Util;

public class SMT {
	private Context ctx;
	private Optimize opt;
	private PreprocessingResult pre;
	private IntExpr costFunction;
	private Config config;
	private List<AnalysisConstraint> constraints;
	Map<Node, Map<Label, BoolExpr>> nodeLabelRef = new HashMap<>();
	Map<Node, Map<Label, BoolExpr>> nodeLabels = new HashMap<>();
	public Map<TFGFlow, Map<Label, BoolExpr>> flowLabels;
	Map<Pin, Map<Label, BoolExpr>> pinSet;
	Map<Pin, Map<Label, BoolExpr>> pinUnset;
	public Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows;
	public Map<Pin, List<AbstractAssignment>> outPinToAss;

	public SMT(PreprocessingResult pre, List<AnalysisConstraint> constraints, Config config) {
		this.config = config;
		this.constraints = constraints;
		this.ctx = new Context();
		this.opt = ctx.mkOptimize();
		this.pre = pre;
		vertexIncomingFlows = pre.vertexIncomingFlows();
		nodeLabelRef = new HashMap<>();
		nodeLabels = new HashMap<>();
		flowLabels = new HashMap<>();
		pinSet = new HashMap<>();
		pinUnset = new HashMap<>();
		outPinToAss = Util.outPinToAss(pre.dfd().dataFlowDiagram().getNodes());

		initializeStructure();
		CostFunction costFunctionBuilder = CostFunction.create(ctx);
		CostConfig costConfig = config.getCostConfig();

		if (costConfig.isWeighTFGs()) {
			HashMap<Node, Integer> nodeWeights = new HashMap<>();
			HashMap<Pin, Integer> pinWeights = new HashMap<>();
			for (DFDVertex vertex : pre.vertices()) {
				nodeWeights.put(vertex.getReferencedElement(),
						nodeWeights.getOrDefault(vertex.getReferencedElement(), 0) + 1);
				for (Pin pin : vertex.getPinFlowMap().keySet()) {
					pinWeights.put(pin, pinWeights.getOrDefault(pin, 0) + 1);
				}
			}
			costConfig.setNodeFactor(nodeWeights);
			costConfig.setPinFactor(pinWeights);
		}

		Map<Label, Integer> addLabelCost = Util.transformLabelCosts(pre.dfd().dataDictionary(),
				costConfig.getAddLabelCost());
		Map<Label, Integer> removeLabelCost = Util.transformLabelCosts(pre.dfd().dataDictionary(),
				costConfig.getRemoveLabelCost());

		for (Entry<Node, Map<Label, BoolExpr>> map : nodeLabelRef.entrySet()) {
			int nodeCost = costConfig.getNodeFactor().getOrDefault(map.getKey(), 1);
			for (Entry<Label, BoolExpr> ref : map.getValue().entrySet()) {
				if (!map.getKey().getProperties().contains(ref.getKey())) {
					costFunctionBuilder.add(nodeLabels.get(map.getKey()).get(ref.getKey()), ref.getValue(),
							addLabelCost.getOrDefault(ref.getKey(), 1) * nodeCost);
				} else {
					costFunctionBuilder.add(nodeLabels.get(map.getKey()).get(ref.getKey()), ref.getValue(),
							removeLabelCost.getOrDefault(ref.getKey(), 1) * nodeCost);
				}
			}
		}
		for (Entry<Pin, Map<Label, BoolExpr>> map : pinSet.entrySet()) {
			int pinCost = costConfig.getPinFactor().getOrDefault(map.getKey(), 1);
			for (Entry<Label, BoolExpr> set : map.getValue().entrySet()) {
				costFunctionBuilder.add(set.getValue(), ctx.mkFalse(),
						addLabelCost.getOrDefault(set.getKey(), 1) * pinCost);
			}
		}
		for (Entry<Pin, Map<Label, BoolExpr>> map : pinUnset.entrySet()) {
			int pinCost = costConfig.getPinFactor().getOrDefault(map.getKey(), 1);
			for (Entry<Label, BoolExpr> unset : map.getValue().entrySet()) {
				costFunctionBuilder.add(unset.getValue(), ctx.mkFalse(),
						removeLabelCost.getOrDefault(unset.getKey(), 1) * pinCost);
			}
		}

		costFunction = costFunctionBuilder.build();
		createDataFlowExpressions();
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
			return new SolvingResult(false, null, null, Integer.MAX_VALUE, Optional.empty(), Optional.empty(),
					solveTime);
		} else {
			Model m = opt.getModel();
			Optional<Long> expressionTreeSize;
			if (config.isFindExpressionTreeSize()) {
				BoolExpr[] assertions = opt.getAssertions();
				long astNodes = SMTUtil.countAstNodes(assertions);
				expressionTreeSize = Optional.of(astNodes);
			} else {
				expressionTreeSize = Optional.empty();
			}
			IntExpr costValExpr = (IntExpr) m.eval(costFunction, true);
			List<Operation> parseActions = parseActions(m);
			DataFlowDiagramAndDictionary dfd = pre.dfd();
			for (int i = 0; i < parseActions.size(); i++) {
				dfd = parseActions.get(i).doOperation(dfd);
			}
			int cost = Integer.parseInt(costValExpr.toString());
			Optional<Integer> violationsAfter;
			if (config.isCheckForViolationsAfter()) {
				violationsAfter = Optional.of(Util.countViolations(dfd, constraints));
			} else {
				violationsAfter = Optional.empty();
			}
			ctx.close();
			return new SolvingResult(true, dfd, parseActions, cost, expressionTreeSize, violationsAfter, solveTime);
		}
	}

	private void createUserConstraints(List<AnalysisConstraint> constraints) {
		TranslationEnv env = new TranslationEnv(ctx, opt, pre.dfd().dataDictionary(), vertexIncomingFlows, flowLabels,
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

	private void createDataFlowExpression(TFGFlow flow) {
		if (flowLabels.get(flow) != null) {
			return;
		} else {
			flow.thisFlowForwards.values().forEach(x -> x.forEach(y -> createDataFlowExpression(y)));
			flow.thisFlowEvaluatesOn.values().forEach(x -> x.forEach(y -> createDataFlowExpression(y)));
			flowLabels.put(flow, new HashMap<>());
			Pin pin = flow.srcPin;
			List<AbstractAssignment> assignments = outPinToAss.get(pin);
			Set<Label> allDataLabels = new HashSet<>();
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

	private void createDataFlowExpressions() {
		Set<TFGFlow> allFlows = pre.flows();
		if ((!pre.relevantDataLabelsAdd().isEmpty() || !pre.relevantDataLabelsRemove().isEmpty())) {
			for (TFGFlow flow : allFlows) {
				createDataFlowExpression(flow);
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

	private void initializeStructure() {
		initializePins();
		initializeNodes();
	}

	private void initializePins() {
		List<Pin> allOutPins = pre.dfd().dataDictionary().getBehavior().stream().flatMap(x -> x.getOutPin().stream())
				.toList();

		Set<Label> dataLabelsAdd = config.isAddDataLabels() ? pre.relevantDataLabelsAdd() : new HashSet<>();
		Set<Label> dataLabelsRemove = config.isRemoveDataLabels() ? pre.relevantDataLabelsRemove() : new HashSet<>();

		if (config.isOnlyRelevantLabels()) {
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

	private void initializeNodes() {
		Set<Label> nodeLabelsAdd = config.isAddNodeLabels() ? pre.relevantNodeLabelsAdd() : new HashSet<>();
		Set<Label> nodeLabelsRemove = config.isRemoveNodeLabels() ? pre.relevantNodeLabelsRemove() : new HashSet<>();
		Set<Label> allNodeLabels = new HashSet<>();
		allNodeLabels.addAll(pre.relevantNodeLabelsAdd());
		allNodeLabels.addAll(pre.relevantNodeLabelsRemove());
		if (config.isOnlyRelevantLabels()) {
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

	private List<Operation> parseActions(Model m) {
		List<Operation> changes = new ArrayList<>();

		for (Node n : nodeLabelRef.keySet()) {
			Map<Label, BoolExpr> beforeMap = nodeLabelRef.get(n);
			Map<Label, BoolExpr> afterMap = nodeLabels.get(n);

			for (Label lbl : beforeMap.keySet()) {
				BoolExpr beforeExpr = beforeMap.get(lbl);
				BoolExpr afterExpr = afterMap.get(lbl);

				boolean beforeVal = ((BoolExpr) m.evaluate(beforeExpr, true)).isTrue();
				boolean afterVal = ((BoolExpr) m.evaluate(afterExpr, true)).isTrue();

				if (!beforeVal && afterVal) {
					changes.add(new NodeLabelAddOperation(n, lbl));
				} else if (beforeVal && !afterVal) {
					changes.add(new NodeLabelRemoveOperation(n, lbl));
				}
			}
		}
		for (Pin p : pinSet.keySet()) {
			Map<Label, BoolExpr> setMap = pinSet.get(p);

			for (Label label : setMap.keySet()) {
				BoolExpr setExpr = setMap != null ? setMap.get(label) : null;
				if (setExpr != null && ((BoolExpr) m.evaluate(setExpr, true)).isTrue()) {
					changes.add(new SetAssignmentOperation(p, label));
				}
			}
		}
		for (Pin p : pinUnset.keySet()) {
			Map<Label, BoolExpr> unsetMap = pinUnset.get(p);
			for (Label label : unsetMap.keySet()) {
				BoolExpr unsetExpr = unsetMap != null ? unsetMap.get(label) : null;

				if (unsetExpr != null && ((BoolExpr) m.evaluate(unsetExpr, true)).isTrue()) {
					changes.add(new UnsetAssignmentOperation(p, label));
				}
			}
		}

		return changes;
	}
}
