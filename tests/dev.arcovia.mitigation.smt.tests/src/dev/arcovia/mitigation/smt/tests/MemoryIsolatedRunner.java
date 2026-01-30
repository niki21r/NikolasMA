package dev.arcovia.mitigation.smt.tests;

import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

import dev.arcovia.mitigation.smt.Main;

public class MemoryIsolatedRunner {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MemoryIsolatedRunner <model> <constraintId>");
            System.exit(2);
        }

        String model = args[0];
        int constraintId = Integer.parseInt(args[1]);

        Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
        List<AnalysisConstraint> constraint = constraintMap.get(constraintId);
        if (constraint == null) {
            System.err.println("Constraint undefined: " + constraintId);
            System.exit(3);
        }

        // The work we measure in the parent by sampling this process's RSS
        DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0");
        Main.run(dfd, constraint, null, null);
    }
}
