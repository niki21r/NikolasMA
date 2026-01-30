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
import dev.arcovia.mitigation.smt.actions.Action;
import dev.arcovia.mitigation.smt.util.Util;

public class ModificationsTest {

	@Test
	public void testAllForModifications() throws Exception {
		try {
			var tuhhModels = TuhhModels.getTuhhModels();

			List<ModificationsResult> modificationsResults = new ArrayList<ModificationsResult>();
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
					DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0");
					System.out.println("Running " + model + " with constraints " + i);
					List<Action> suggestedActions = Main.run(dfd, constraint, null, null).repairActions();
					int removeableActions = 0;
					for (int j = 0; j < suggestedActions.size(); j++) {
						Action action = suggestedActions.get(j);
						System.out.println("Removed "+suggestedActions.get(j));
						dfd = action.undoAction(dfd);
						if (Util.countViolations(dfd, constraint) <= 0) {
							System.out.println("Found no violation");
							removeableActions++;
							System.exit(1);
						}
						dfd = action.doAction(dfd);
						System.out.println("Applied "+suggestedActions.get(j));
					}
					ModificationsResult result = new ModificationsResult(model, i, suggestedActions.size(),
							removeableActions);
					modificationsResults.add(result);
				}
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/modificationResults/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), modificationsResults);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private record ModificationsResult(String model, int constraints, int modificationCount, int removeableActions) {
	}

}
