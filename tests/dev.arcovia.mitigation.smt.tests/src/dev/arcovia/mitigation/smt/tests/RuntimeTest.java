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

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.CostConfigBuilder;
import dev.arcovia.mitigation.smt.util.Util;

public class RuntimeTest {

	@Test
	public void testAllForRuntime() throws Exception {
		try {
			var tuhhModels = TuhhModels.getTuhhModels();

			List<RuntimeResult> runtimeResults = new ArrayList<RuntimeResult>();
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
					Config config = new Config(true, true, true, true, true, new CostConfigBuilder().build(), false, true);
					long dagSizeAfter = Mitigation.run(Util.loadDFD(model, model+"_0"), constraint, config).expressionTreeSize().get();
					RuntimeResult runtimeResult = new RuntimeResult(dagSizeAfter, new ArrayList<>());
					int totalRuns = 100;
					for (int j = 0; j < totalRuns; j++) {
						System.out.println("Running " + model + " with constraints " + i);
						long before = System.currentTimeMillis();
						DataFlowDiagramAndDictionary dfd = Util.loadDFD(model, model + "_0");
						Mitigation.run(dfd, constraint, null);
						long after = System.currentTimeMillis();
						runtimeResult.averageRuntime.add(after-before);
						System.out.println("Total Runtime: "+(after-before));
					}
					runtimeResults.add(runtimeResult);
				}
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/runtimeResults/100runs/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), runtimeResults);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private record RuntimeResult(long dagSize, List<Long> averageRuntime) {
	}

}
