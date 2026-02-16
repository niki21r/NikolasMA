package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.util.Util;

final class VertexCharacteristicsHandler extends AbstractSelectorHandler<VertexCharacteristicsSelector> {
	@Override
	protected BoolExpr encode(VertexCharacteristicsSelector s, DFDVertex vertex, SelectorRole role, SMT smt) {

		return switch (role) {
		case VERTEX_DESTINATION -> matchesDestinationVertexCharacteristics(s, vertex, smt);
		case VERTEX_SOURCE -> matchesSourceVertexCharacteristics(s, vertex, smt);
		case DATA_SOURCE -> throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		};
	}

	private BoolExpr matchesDestinationVertexCharacteristics(VertexCharacteristicsSelector s, DFDVertex vertex,
			SMT smt) {
		var ctx = smt.getCtx();

		Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDD(),
				List.of(s.getVertexCharacteristics()));

		Map<Label, BoolExpr> present = smt.getNodeLabels().get(vertex.getReferencedElement());

		List<BoolExpr> labelMatches = new ArrayList<>(selectorLabels.size());
		for (Label lbl : selectorLabels) {
			BoolExpr has = (present != null) ? present.get(lbl) : null;
			labelMatches.add(has != null ? has : ctx.mkFalse());
		}

		BoolExpr matches = labelMatches.isEmpty() ? ctx.mkFalse() : ctx.mkOr(labelMatches.toArray(new BoolExpr[0]));

		BoolExpr result = s.isInverted() ? ctx.mkNot(matches) : matches;

		return result;
	}

	private BoolExpr matchesSourceVertexCharacteristics(VertexCharacteristicsSelector s, DFDVertex vertex, SMT smt) {
		List<BoolExpr> matches = new ArrayList<>();
		matches.add(matchesDestinationVertexCharacteristics(s, vertex, smt));

		for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
			DFDVertex prev = (DFDVertex) prevAbstract;
			matches.add(matchesDestinationVertexCharacteristics(s, prev, smt));
		}

		BoolExpr anyMatch = smt.getCtx().mkOr(matches.toArray(new BoolExpr[0]));
		return s.isInverted() ? smt.getCtx().mkNot(anyMatch) : anyMatch;
	}
}
