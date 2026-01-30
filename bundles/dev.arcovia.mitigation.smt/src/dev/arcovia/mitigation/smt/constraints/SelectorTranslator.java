package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;

public interface SelectorTranslator {
	public BoolExpr toBool(AbstractSelector selector, DFDVertex vertex, SelectorRole role);
}
