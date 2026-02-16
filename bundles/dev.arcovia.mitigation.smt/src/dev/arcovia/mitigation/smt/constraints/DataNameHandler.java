package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.TFGFlow;

final class DataNameHandler implements SelectorHandler<VariableNameSelector> {
	@Override
	public BoolExpr encode(VariableNameSelector s, DFDVertex vertex, SelectorRole role, TranslationEnv env) {

		if (role != SelectorRole.DATA_SOURCE) {
			throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		}

		var ctx = env.ctx();

		String selectorName = s.getVariableName();

		List<BoolExpr> flowsMatch = new ArrayList<>();
		for (TFGFlow flow : env.vertexIncomingFlows().getOrDefault(vertex, List.of())) {
			if (flow.flow.getEntityName().equals(selectorName)) {
				flowsMatch.add(ctx.mkTrue());
			} else {
				flowsMatch.add(ctx.mkFalse());
			}
		}

		if (flowsMatch.isEmpty()) {
			return ctx.mkFalse();
		}

		BoolExpr anyFlowMatches = ctx.mkOr(flowsMatch.toArray(new BoolExpr[0]));
		return anyFlowMatches;
	}

}
