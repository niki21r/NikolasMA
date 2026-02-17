package dev.arcovia.mitigation.smt.tests.evaluation;

import java.util.List;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.util.Util;

public class NewTest {

	@Test
	public void newTest() throws Exception {
		DataFlowDiagramAndDictionary dfd = Util.loadDFD("koushikkothagal", "koushikkothagal_0");
		List<AnalysisConstraint> constraints = ConstraintMapProvider.getOrThrow(4);

		System.out.println(Util.countViolations(dfd, constraints));
		System.out.println(Mitigation.run(dfd, constraints, null));
	}
}
