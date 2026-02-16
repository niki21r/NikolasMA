package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dfd.dsl.DFDVertexType;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.util.Util;

final class VertexTypeHandler implements SelectorHandler<VertexTypeSelector> {

	@Override
	public BoolExpr encode(VertexTypeSelector s, DFDVertex vertex, SelectorRole role, TranslationEnv env) {

		return switch (role) {
		case VERTEX_DESTINATION -> matchesDestinationVertexType(s, vertex, env);
		case VERTEX_SOURCE -> matchesSourceVertexType(s, vertex, env);
		case DATA_SOURCE -> throw new UnsupportedOperationException("DATA_SOURCE is not supported for vertex encoding");
		};
	}

	private BoolExpr matchesDestinationVertexType(VertexTypeSelector s, DFDVertex vertex, TranslationEnv env) {
		var ctx = env.ctx();

		DFDVertexType selectorType = (DFDVertexType) s.getVertexType();

		BoolExpr matches;
		if (selectorType.equals(Util.vertexToType(vertex))) {
			matches = ctx.mkTrue();
		} else {
			matches = ctx.mkFalse();
		}
		return s.isInverted() ? ctx.mkNot(matches) : matches;
	}

	private BoolExpr matchesSourceVertexType(VertexTypeSelector s, DFDVertex vertex, TranslationEnv env) {
		List<BoolExpr> matches = new ArrayList<>();
		matches.add(matchesDestinationVertexType(s, vertex, env));
		for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
			DFDVertex prev = (DFDVertex) prevAbstract;
			matches.add(matchesDestinationVertexType(s, prev, env));
		}
		BoolExpr anyMatch = env.ctx().mkOr(matches.toArray(new BoolExpr[0]));
		return s.isInverted() ? env.ctx().mkNot(anyMatch) : anyMatch;
	}
}
