package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * Selector translation logic for VertexTypeSelectors
 * @author Nikolas Rank
 *
 */
final class VertexTypeHandler extends AbstractSelectorHandler<VertexTypeSelector> {

	@Override
	protected BoolExpr encode(VertexTypeSelector s, DFDVertex vertex, SelectorRole role, SMT smt) {

		return switch (role) {
		case VERTEX_DESTINATION -> matchesDestinationVertexType(s, vertex, smt);
		case VERTEX_SOURCE -> matchesSourceVertexType(s, vertex, smt);
		case DATA_SOURCE -> throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		};
	}

	/**
	 * Base case for singular vertices
	 * @param s Selector 
	 * @param vertex Vertex
	 * @param smt 
	 * @return Expression that denotes whether this selector matches
	 */
	private BoolExpr matchesDestinationVertexType(VertexTypeSelector s, DFDVertex vertex, SMT smt) {
		var ctx = smt.getCtx();

		DFDVertexType selectorType = (DFDVertexType) s.getVertexType();

		// Can be statically evaluated at encoding time because types are not modifiable
		BoolExpr matches;
		if (selectorType.equals(Util.vertexToType(vertex))) {
			matches = ctx.mkTrue();
		} else {
			matches = ctx.mkFalse();
		}
		// Maybe invert
		return s.isInverted() ? ctx.mkNot(matches) : matches;
	}

	/**
	 * Recursive case for source selectors.
	 * @param s Selector
	 * @param vertex Vertex
	 * @param smt 
	 * @return Expression that denotes whether this vertex matches
	 */
	private BoolExpr matchesSourceVertexType(VertexTypeSelector s, DFDVertex vertex, SMT smt) {
		List<BoolExpr> matches = new ArrayList<>();
		// Matches if the vertex itself matches
		matches.add(matchesDestinationVertexType(s, vertex, smt));
		// Or any preceeding vertices
		for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
			DFDVertex prev = (DFDVertex) prevAbstract;
			matches.add(matchesDestinationVertexType(s, prev, smt));
		}
		BoolExpr anyMatch = smt.getCtx().mkOr(matches.toArray(new BoolExpr[0]));
		// Maybe invert
		return s.isInverted() ? smt.getCtx().mkNot(anyMatch) : anyMatch;
	}
}
