package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.util.Util;

final class DataCharacteristicsHandler implements SelectorHandler<DataCharacteristicsSelector> {
	@Override
	public BoolExpr encode(
	        DataCharacteristicsSelector s,
	        DFDVertex vertex,
	        SelectorRole role,
	        TranslationEnv env
	) {
	    if (role != SelectorRole.DATA_SOURCE) {
	        throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
	    }

	    var ctx = env.ctx();

	    List<Label> selectorLabels = Util.getLabelsForCharacteristics(
	            env.pre().dfd().dataDictionary(),
	            List.of(s.getDataCharacteristic())
	    );

	    List<BoolExpr> flowsMatch = new ArrayList<>();

	    for (TFGFlow flow : env.vertexIncomingFlows().getOrDefault(vertex, List.of())) {
	        Map<Label, BoolExpr> flowLabelMap = env.flowLabels().get(flow);

	        List<BoolExpr> anySelectorLabelPresent = new ArrayList<>(selectorLabels.size());
	        for (Label lbl : selectorLabels) {
	            BoolExpr has = (flowLabelMap != null) ? flowLabelMap.get(lbl) : null;
	            anySelectorLabelPresent.add(has != null ? has : ctx.mkFalse());
	        }

	        BoolExpr thisFlowMatches = anySelectorLabelPresent.isEmpty()
	                ? ctx.mkFalse()
	                : ctx.mkOr(anySelectorLabelPresent.toArray(new BoolExpr[0]));

	        flowsMatch.add(thisFlowMatches);
	    }

	    if (flowsMatch.isEmpty()) {
	        return ctx.mkFalse();
	    }

	    BoolExpr anyFlowMatches = ctx.mkOr(flowsMatch.toArray(new BoolExpr[0]));
	    return s.isInverted() ? ctx.mkNot(anyFlowMatches) : anyFlowMatches;
	}
}
