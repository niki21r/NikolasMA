package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

public class VertexNameHandler extends AbstractSelectorHandler<VertexNameSelector> {

	@Override
	protected BoolExpr encode(VertexNameSelector selector, DFDVertex vertex, SelectorRole role, SMT smt) {

		if (role != SelectorRole.VERTEX_DESTINATION) {
			throw new UnsupportedOperationException(
					"Roles other than Vertex Destination are not supported for vertex encoding");
		}

		var ctx = smt.getCtx();

		String select = selector.getName();
		BoolExpr matches;
		if (vertex.getReferencedElement().getEntityName().equals(select)) {
			matches = ctx.mkTrue();
		} else {
			matches = ctx.mkFalse();
		}
		return selector.isInverted() ? ctx.mkNot(matches) : matches;
	}

}
