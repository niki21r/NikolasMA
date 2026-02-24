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

/**
 * Selector translation logic for VertexCharacteristicsSelector
 * @author Nikolas Rank
 *
 */
final class VertexCharacteristicsHandler extends AbstractSelectorHandler<VertexCharacteristicsSelector> {
	@Override
	protected BoolExpr encode(VertexCharacteristicsSelector s, DFDVertex vertex, SelectorRole role, SMT smt) {

		return switch (role) {
		case VERTEX_DESTINATION -> matchesDestinationVertexCharacteristics(s, vertex, smt);
		case VERTEX_SOURCE -> matchesSourceVertexCharacteristics(s, vertex, smt);
		case DATA_SOURCE -> throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		};
	}

	/**
	 * Base case for singular vertices.
	 * @param s Selector 
	 * @param vertex Vertex 
	 * @param smt for access to node labels
	 * @return Expression that denotes whether this vertex matches
	 */
	private BoolExpr matchesDestinationVertexCharacteristics(VertexCharacteristicsSelector s, DFDVertex vertex,
			SMT smt) {
		var ctx = smt.getCtx();

		//Set only contains one label
		Set<Label> selectorLabels = Util.getLabelsForCharacteristics(smt.getDD(),
				List.of(s.getVertexCharacteristics()));

		// Get labels for node
		Map<Label, BoolExpr> present = smt.getNodeLabels().get(vertex.getReferencedElement());

		List<BoolExpr> labelMatches = new ArrayList<>(selectorLabels.size());
		for (Label lbl : selectorLabels) {
			BoolExpr has = present.get(lbl);
			labelMatches.add(has);
		}

		BoolExpr matches = ctx.mkOr(labelMatches.toArray(new BoolExpr[0]));

		BoolExpr result = s.isInverted() ? ctx.mkNot(matches) : matches;

		return result;
	}

	/**
	 * Recursive case for vertex source selectors
	 * @param s Selector 
	 * @param vertex Vertex 
	 * @param smt for access to node labels
	 * @return Expression that denotes whether the vertex matches
	 */
	private BoolExpr matchesSourceVertexCharacteristics(VertexCharacteristicsSelector s, DFDVertex vertex, SMT smt) {
		List<BoolExpr> matches = new ArrayList<>();
		// Matches if it has label
		matches.add(matchesDestinationVertexCharacteristics(s, vertex, smt));

		// Or if any preceeding vertex has label
		for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
			DFDVertex prev = (DFDVertex) prevAbstract;
			matches.add(matchesDestinationVertexCharacteristics(s, prev, smt));
		}

		BoolExpr anyMatch = smt.getCtx().mkOr(matches.toArray(new BoolExpr[0]));
		// Maybe invert
		return s.isInverted() ? smt.getCtx().mkNot(anyMatch) : anyMatch;
	}
}
