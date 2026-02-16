package dev.arcovia.mitigation.smt.tests.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.DFDConfidentialityAnalysis;
import org.dataflowanalysis.analysis.dfd.DFDDataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.dfd.core.DFDFlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.resource.DFDModelResourceProvider;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.result.DSLResult;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.util.Util;
public class NewTest {

	@Test
	public void newTest() throws Exception {
		try {
			DataFlowDiagramAndDictionary dfd = Util.loadDFD("koushikkothagal", "koushikkothagal_0");
			List<AnalysisConstraint> constraints = ConstraintMapProvider.buildConstraintMap().get(2);
			HashMap<String, Integer> labelCost = new HashMap<>();
			labelCost.put("Stereotype.internal", 5);
			countViolations(dfd, constraints);
			System.out.println(Mitigation.run(dfd, constraints, null));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	
	public static int countViolations(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> analysisConstraints) {
		DFDModelResourceProvider dfdModelResourceProvider = new DFDModelResourceProvider(dfd.dataDictionary(),
				dfd.dataFlowDiagram());
		DFDConfidentialityAnalysis dfdConfidentialityAnalysis = new DFDDataFlowAnalysisBuilder().standalone()
				.useCustomResourceProvider(dfdModelResourceProvider).build();
		DFDFlowGraphCollection flowGraphs = dfdConfidentialityAnalysis.findFlowGraphs();
		flowGraphs.evaluate();
		List<AbstractTransposeFlowGraph> tfgs = (List<AbstractTransposeFlowGraph>) flowGraphs.getTransposeFlowGraphs();
		for (int i = 0; i < flowGraphs.getTransposeFlowGraphs().size(); i++) {
			System.out.println(tfgs.get(i));
		}
				
		List<DSLResult> result = new ArrayList<>();
		for (int i = 0; i < analysisConstraints.size(); i++) {
			List<DSLResult> violations = analysisConstraints.get(i).findViolations(flowGraphs);
			result.addAll(violations);
		}
		if (result.size() > 0) {
			System.out.println("DFA found " + result.size() + " tfg violations");
			return result.size();
		} else {
			// System.out.println("No confidentiality violations found.");
			return 0;
		}

	}

}
