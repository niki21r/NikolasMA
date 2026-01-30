package dev.arcovia.mitigation.smt;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

public record Result(DataFlowDiagramAndDictionary repairedDfd, long solveTime) {
}
