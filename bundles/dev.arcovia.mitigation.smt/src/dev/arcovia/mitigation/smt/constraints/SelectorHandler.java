package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;

interface SelectorHandler<T extends AbstractSelector> {
	BoolExpr encode(T selector, DFDVertex vertex, SelectorRole role, TranslationEnv env);
}
