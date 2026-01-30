package dev.arcovia.mitigation.smt.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.util.Util;

final class VertexCharacteristicListHandler implements SelectorHandler<VertexCharacteristicsListSelector> {
	
	@Override
	public BoolExpr encode(
	        VertexCharacteristicsListSelector s,
	        DFDVertex vertex,
	        SelectorRole role,
	        TranslationEnv env
	) {
	    return switch (role) {
	        case VERTEX_DESTINATION ->
	                matchesDestinationVertexCharacteristicsList(s, vertex, env);
	        case VERTEX_SOURCE ->
	                matchesSourceVertexCharacteristicsList(s, vertex, env);
	        case DATA_SOURCE ->
	                throw new UnsupportedOperationException(
	                        "DATA_SOURCE is not supported for vertex encoding"
	                );
	    };
	}

	private BoolExpr matchesDestinationVertexCharacteristicsList(
	        VertexCharacteristicsListSelector s,
	        DFDVertex vertex,
	        TranslationEnv env
	) {
	    var ctx = env.ctx();
	    
	    List<Label> selectorLabels = Util.getLabelsForCharacteristics(
	            env.pre().dfd().dataDictionary(),
	            s.getVertexCharacteristics()
	    );

	    Map<Label, BoolExpr> present =
	            env.nodeLabels().get(vertex.getReferencedElement());

	    List<BoolExpr> labelMatches = new ArrayList<>(selectorLabels.size());
	    for (Label lbl : selectorLabels) {
	        BoolExpr has = (present != null) ? present.get(lbl) : null;
	        labelMatches.add(has != null ? has : ctx.mkFalse());
	    }

	    BoolExpr matches = labelMatches.isEmpty()
	            ? ctx.mkFalse()
	            : ctx.mkOr(labelMatches.toArray(new BoolExpr[0]));

	    BoolExpr result = s.isInverted() ? ctx.mkNot(matches) : matches;
	    	    
	    return result;
	}

	private BoolExpr matchesSourceVertexCharacteristicsList(
	        VertexCharacteristicsListSelector s,
	        DFDVertex vertex,
	        TranslationEnv env
	) {
	    List<BoolExpr> matches = new ArrayList<>();

	    matches.add(matchesDestinationVertexCharacteristicsList(s, vertex, env));

	    for (AbstractVertex<?> prevAbstract : vertex.getPreviousElements()) {
	        DFDVertex prev = (DFDVertex) prevAbstract;
	        matches.add(matchesDestinationVertexCharacteristicsList(s, prev, env));
	    }

	    BoolExpr anyMatch = env.ctx().mkOr(matches.toArray(new BoolExpr[0]));
	    return s.isInverted() ? env.ctx().mkNot(anyMatch) : anyMatch;
	}

}
