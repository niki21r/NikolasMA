package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.util.Util;

final class DataCharacteristicListHandler extends AbstractSelectorHandler<DataCharacteristicListSelector> {
	@Override
	protected BoolExpr encode(DataCharacteristicListSelector s, DFDVertex vertex, SelectorRole role, SMT smt) {
		if (role != SelectorRole.DATA_SOURCE) {
			throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		}

		var ctx = smt.getCtx();

		Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDD(), s.getDataCharacteristics());

		List<BoolExpr> flowsMatch = new ArrayList<>();

		for (TFGFlow flow : smt.getVertexIncomingFlows().getOrDefault(vertex, List.of())) {
			Map<Label, BoolExpr> flowLabelMap = smt.getFlowLabels().get(flow);

			List<BoolExpr> anySelectorLabelPresent = new ArrayList<>(selectorLabels.size());
			for (Label lbl : selectorLabels) {
				BoolExpr has = flowLabelMap.get(lbl);
				anySelectorLabelPresent.add(has);
			}

			BoolExpr thisFlowMatches = ctx.mkOr(anySelectorLabelPresent.toArray(new BoolExpr[0]));

			flowsMatch.add(thisFlowMatches);
		}

		if (flowsMatch.isEmpty()) {
			return ctx.mkFalse();
		}

		BoolExpr anyFlowMatches = ctx.mkOr(flowsMatch.toArray(new BoolExpr[0]));
		return s.isInverted() ? ctx.mkNot(anyFlowMatches) : anyFlowMatches;
	}
}
