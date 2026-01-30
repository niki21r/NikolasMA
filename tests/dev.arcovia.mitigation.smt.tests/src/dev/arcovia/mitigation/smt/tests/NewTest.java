package dev.arcovia.mitigation.smt.tests;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Main;
public class NewTest {

	@Test
	public void newTest() throws Exception {
		try {
			DataFlowDiagramAndDictionary dfd = Main.loadDFD("anilallewar", "anilallewar_0");
			List<AnalysisConstraint> constraints = ConstraintMapProvider.buildConstraintMap().get(7);
			Main.run(dfd, constraints, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
}
