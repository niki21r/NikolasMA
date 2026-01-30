package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.IntNum;

public class VertexNameHandler implements SelectorHandler<VertexNameSelector>{

	@Override
	public BoolExpr encode(VertexNameSelector selector, DFDVertex vertex, SelectorRole role, TranslationEnv env) {

		if (role != SelectorRole.VERTEX_DESTINATION) {
			throw new UnsupportedOperationException("Roles other than Vertex Destination are not supported for vertex encoding");
		}

		var ctx = env.ctx();

		String select = selector.getName();
		IntNum selectorName = ctx.mkInt(env.mappings().nodeNameToInt.get(select));
		IntNum vertexName = ctx.mkInt(env.mappings().nodeNameToInt.get(vertex.getName()));
		
		BoolExpr matches = ctx.mkEq(selectorName, vertexName);
		return selector.isInverted() ? ctx.mkNot(matches) : matches;
	}


		

	

}
