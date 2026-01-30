package dev.arcovia.mitigation.smt.constraints;

import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Optimize;

import dev.arcovia.mitigation.smt.SMTMappings;
import dev.arcovia.mitigation.smt.TFGFlow;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;

public record TranslationEnv(Context ctx, Optimize opt, PreprocessingResult pre, SMTMappings mappings,
		Map<DFDVertex, List<TFGFlow>> vertexIncomingFlows, Map<TFGFlow, Map<Label, BoolExpr>> flowLabels, Map<TFGFlow, IntNum> flowNames,
		Map<Node, Map<Label, BoolExpr>> nodeLabels) {
}
