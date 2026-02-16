package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import dev.arcovia.mitigation.smt.SMT;

public class ConstraintTranslator {
	private final DefaultSelectorTranslator selectorTranslator;
	private final SMT smt;

	public ConstraintTranslator(SMT smt) {
		this.selectorTranslator = new DefaultSelectorTranslator(smt);
		this.smt = smt;
	}

	public BoolExpr translateConstraint(AnalysisConstraint constr, DFDVertex vertex) {
		List<AbstractSelector> dataSource = constr.getDataSourceSelectors().getSelectors();
		List<AbstractSelector> vertexSource = constr.getVertexSourceSelectors().getSelectors();
		List<AbstractSelector> vertexDestination = constr.getVertexDestinationSelectors().getSelectors();

		Context ctx = smt.getCtx();

		List<BoolExpr> allDestinationSelectors = new ArrayList<>();
		for (AbstractSelector dstSelector : vertexDestination) {
			allDestinationSelectors
					.add(selectorTranslator.toBool(dstSelector, vertex, SelectorRole.VERTEX_DESTINATION));
		}
		BoolExpr allDestinationSatisfied = ctx.mkAnd(allDestinationSelectors.toArray(new BoolExpr[0]));

		List<BoolExpr> allDataSource = new ArrayList<>();
		for (AbstractSelector source : dataSource) {
			allDataSource.add(selectorTranslator.toBool(source, vertex, SelectorRole.DATA_SOURCE));
		}
		BoolExpr allDataSourceSatisfied = ctx.mkAnd(allDataSource.toArray(new BoolExpr[0]));

		List<BoolExpr> allVertexSource = new ArrayList<>();
		for (AbstractSelector source : vertexSource) {
			allVertexSource.add(selectorTranslator.toBool(source, vertex, SelectorRole.VERTEX_SOURCE));
		}
		BoolExpr allVertexSourceSatisfied = ctx.mkAnd(allVertexSource.toArray(new BoolExpr[0]));

		BoolExpr allSatisfied = ctx.mkAnd(allDestinationSatisfied, allDataSourceSatisfied, allVertexSourceSatisfied);
		BoolExpr notAllSatisfied = ctx.mkNot(allSatisfied);

		return notAllSatisfied;
	}

}
