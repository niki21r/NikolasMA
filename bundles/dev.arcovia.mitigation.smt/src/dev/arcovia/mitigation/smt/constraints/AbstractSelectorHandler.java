package dev.arcovia.mitigation.smt.constraints;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

abstract class AbstractSelectorHandler<T extends AbstractSelector> {
	abstract protected BoolExpr encode(T selector, DFDVertex vertex, SelectorRole role, SMT smt);
}
