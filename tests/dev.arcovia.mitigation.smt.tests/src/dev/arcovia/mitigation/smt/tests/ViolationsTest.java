package dev.arcovia.mitigation.smt.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.smt.Main;
import dev.arcovia.mitigation.smt.util.Util;

public class ViolationsTest {

	@Test
	public void testAllForViolations() throws Exception {
		try {
			var tuhhModels = TuhhModels.getTuhhModels();

			List<ViolationsResult> violationResults = new ArrayList<ViolationsResult>();
			Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();

			int totalViolations = 0;
			
			for (var model : tuhhModels.keySet()) {
				if (!tuhhModels.get(model).contains(0))
					continue;
				for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
					List<AnalysisConstraint> constraint = constraintMap.get(i);
					if (constraint == null) {
						System.out.println("Skipping "+model+" with constraint "+i+" because Constraint is undefined");
						continue;
					} else if (!tuhhModels.get(model).contains(i)) {
						System.out.println("Skipping "+model+" with constraint "+i+" because no model for this constraint is defined");
						continue;
					}
					DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0");
					int violationsBefore = Util.countViolations(dfd, constraint);
					totalViolations += violationsBefore;
					System.out.println("Running " + model + " with constraints " + i);
					DataFlowDiagramAndDictionary repairedDFD = Main.run(dfd, constraint, null, null).repairedDFD();
					int violationsAfter = Util.countViolations(repairedDFD, constraint);
					if (violationsAfter > 0) {
						System.out.println("Violations after repair still present. Fatal");
						System.exit(1);
					}
					ViolationsResult result = new ViolationsResult(model, i, violationsBefore, violationsAfter);
					violationResults.add(result);
				}
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/violationResults/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), violationResults);
			System.out.println("Total violations "+totalViolations);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private record ViolationsResult(String model, int constraints, int violationsBefore, int violationsAfter) {
	}

}
