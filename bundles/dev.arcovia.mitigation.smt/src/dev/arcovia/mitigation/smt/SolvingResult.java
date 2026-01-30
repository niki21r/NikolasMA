package dev.arcovia.mitigation.smt;

import java.util.List;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.smt.actions.Action;

public record SolvingResult(boolean satisfiable, DataFlowDiagramAndDictionary repairedDFD, List<Action> repairActions,
		int repairCost) {
}
