package dev.arcovia.mitigation.smt.tests;

import static java.util.Map.entry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.IncomingDataLabel;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Literal;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;
import dev.arcovia.mitigation.smt.Main;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class RuntimeComparisonTest {

	static final int RUNS_PER_CONFIGURATION = 100;

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

					int dagSizeAfter = (int) Main.findDagSize(Main.loadDFD(model, model + "_0"), constraint);

					List<Long> smtRuntimes = new ArrayList<>();
					List<Long> satRuntimes = new ArrayList<>();

					for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
						//System.out.println("Running " + model + " with constraints " + i);
						long before = System.currentTimeMillis();
						DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0");
						Main.run(dfd, constraint, null, null);
						long after = System.currentTimeMillis();
						long totalRuntime = (after - before);
						smtRuntimes.add(totalRuntime);
					}
					for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
						long before = System.currentTimeMillis();
						var satConstraint = constraint.stream()
								.flatMap(x -> new CNFTranslation(x).constructCNF().stream()).toList();
						runRepair(model, model + "_0", false, satConstraint, minCosts);
						long after = System.currentTimeMillis();
						satRuntimes.add((after-before));
					}

					int clauseCount = extractClauseCount("testresults/aName.cnf");

					RuntimeResult runtimeResult = new RuntimeResult(dagSizeAfter, clauseCount, smtRuntimes,
							satRuntimes);
					runtimeResults.add(runtimeResult);
				}
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/runtimeResults/comparison/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), runtimeResults);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private void runRepair(String model, String name, Boolean store, List<Constraint> constraints,
			Map<Label, Integer> costMap)
			throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
		var dfd = Main.loadDFD(model, name);
		if (!store)
			name = "aName";
		ArrayList<Boolean> complexityReductions = new ArrayList<>();
		complexityReductions.add(true);
		complexityReductions.add(false);
		complexityReductions.add(false);
		complexityReductions.add(false);
		Mechanic mechanic = new Mechanic(dfd, name, constraints, costMap, complexityReductions);
		var repairedDfd = mechanic.repair();
		//int violationsAfter = new Mechanic(repairedDfd, null, null).amountOfViolations(repairedDfd, constraints);
		//return new RepairResult(repairedDfd, mechanic.getViolations(), violationsAfter, endTime - startTime);
		return;
	}

	private record RepairResult(DataFlowDiagramAndDictionary repairedDfd, int violationsBefore, int violationsAfter,
			long runtimeInMilliseconds) {
	}

	private record RuntimeResult(int dagSize, int clauseCount, List<Long> runtimesSMT, List<Long> runtimesSAT) {
	}

	final Constraint entryViaGatewayOnly = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
					new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
					new Literal(true, new IncomingDataLabel(new Label("Stereotype", "gateway")))));
	final Constraint nonInternalGateway = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "gateway"))),
					new Literal(false, new NodeLabel(new Label("Stereotype", "internal")))));
	final Constraint authenticatedRequest = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
					new Literal(true, new IncomingDataLabel(new Label("Stereotype", "authenticated_request")))));
	final Constraint transformedEntry = new Constraint(List.of(
			new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
			new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
			new Literal(true, new IncomingDataLabel(new Label("Stereotype", "transform_identity_representation")))));
	final Constraint tokenValidation = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
					new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
					new Literal(true, new IncomingDataLabel(new Label("Stereotype", "token_validation")))));
	final Constraint loginAttempts = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "authorization_server"))),
					new Literal(true, new NodeLabel(new Label("Stereotype", "login_attempts_regulation")))));
	final Constraint encryptedEntry = new Constraint(
			List.of(new Literal(false, new IncomingDataLabel(new Label("Stereotype", "entrypoint"))),
					new Literal(true, new IncomingDataLabel(new Label("Stereotype", "encrypted_connection")))));
	final Constraint encryptedInternals = new Constraint(
			List.of(new Literal(false, new IncomingDataLabel(new Label("Stereotype", "internal"))),
					new Literal(true, new IncomingDataLabel(new Label("Stereotype", "encrypted_connection")))));
	final Constraint localLogging = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "internal"))),
					new Literal(true, new NodeLabel(new Label("Stereotype", "local_logging")))));
	final Constraint logSanitization = new Constraint(
			List.of(new Literal(false, new NodeLabel(new Label("Stereotype", "local_logging"))),
					new Literal(true, new NodeLabel(new Label("Stereotype", "log_sanitization")))));
	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

	private int extractClauseCount(String filePath) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String firstLine = reader.readLine();
			if (firstLine != null && firstLine.startsWith("p cnf")) {
				String[] parts = firstLine.trim().split("\\s+");
				if (parts.length == 4) {
					return Integer.parseInt(parts[3]);
				}
			}
		}
		throw new IllegalArgumentException("First line is not in the expected 'p cnf <vars> <clauses>' format.");
	}

}
