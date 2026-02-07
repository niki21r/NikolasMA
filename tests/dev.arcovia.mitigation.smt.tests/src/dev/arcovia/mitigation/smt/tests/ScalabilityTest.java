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
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.util.Util;

public class ScalabilityTest {

	@Test
	public void testAllForRuntime() throws Exception {
		try {
			var tuhhModels = TuhhModels.getTuhhModels();

			List<ScalabilityResult> scalabilityResults = new ArrayList<ScalabilityResult>();
			Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();
			for (var model : tuhhModels.keySet()) {
				if (!tuhhModels.get(model).contains(0))
					continue;
				for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
					List<AnalysisConstraint> constraint = constraintMap.get(i);
					if (constraint == null) {
						System.out.println(
								"Skipping " + model + " with constraint " + i + " because Constraint is undefined");
						continue;
					} else if (!tuhhModels.get(model).contains(i)) {
						System.out.println("Skipping " + model + " with constraint " + i
								+ " because no model for this constraint is defined");
						continue;
					}
					ScalabilityResult runtimeResult = new ScalabilityResult(new ArrayList<>(), new ArrayList<>());
					int totalRuns = 100;
					for (int j = 0; j < totalRuns; j++) {
						long before = System.currentTimeMillis();
						DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0");
						SolvingResult solvingResult = Main.run(dfd, constraint, null);
						long after = System.currentTimeMillis();
						runtimeResult.runtimesSMT.add(after-before);
						System.out.println("Total Runtime: "+(after-before));
						if (!solvingResult.satisfiable() || Util.countViolations(solvingResult.repairedDFD(), constraint) > 0) {
							System.out.println("Found error");
							System.exit(1);
						};
					}
					scalabilityResults.add(runtimeResult);
				}
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/scalabilityResults/100runs/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), scalabilityResults);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private record ScalabilityResult(List<Long> runtimesSMT, List<Long> runtimesSAT) {
	}

}
