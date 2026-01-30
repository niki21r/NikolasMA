package dev.arcovia.mitigation.smt.tests;

import static java.util.Map.entry;

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
import dev.arcovia.mitigation.sat.ModelCostCalculator;
import dev.arcovia.mitigation.sat.ModelCostCalculator2;
import dev.arcovia.mitigation.sat.NodeLabel;
import dev.arcovia.mitigation.smt.Main;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.util.Util;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

public class ModificationCountComparison {
	@Test
	void efficiencyTest()
			throws ContradictionException, TimeoutException, IOException, StandaloneInitializationException {
		var tuhhModels = TuhhModels.getTuhhModels();

		Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();

		List<ComparisonResult> comparisonResults = new ArrayList<ComparisonResult>();

		for (var model : tuhhModels.keySet()) {
			if (!tuhhModels.get(model).contains(0))
				continue;

			System.out.println("Checking " + model);

			for (int variant : tuhhModels.get(model)) {
				List<Constraint> constraint = switch (variant) {
				case 1 -> List.of(entryViaGatewayOnly, nonInternalGateway);
				case 2 -> List.of(authenticatedRequest);
				case 4 -> List.of(transformedEntry);
				case 5 -> List.of(tokenValidation);
				case 7 -> List.of(encryptedEntry, entryViaGatewayOnly, nonInternalGateway);
				case 8 -> List.of(encryptedInternals);
				case 10 -> List.of(localLogging);
				case 11 -> List.of(localLogging, logSanitization);
				default -> null;
				};
				if (constraint == null)
					continue;
				System.out.println("Comparing " + model + "_" + variant);
				var repairResult = runRepair(model, model + "_0", false, constraint, minCosts);
				var repairedDfd = repairResult.repairedDfd();
				boolean useNewModelCost = true;
				int satCost;
				int baseCost;
				if (useNewModelCost) {
					baseCost = new ModelCostCalculator2(Main.loadDFD(model, model+"_0"), constraint, minCosts).calculateCostWithoutForwarding();
					satCost = new ModelCostCalculator2(repairedDfd, constraint, minCosts)
							.calculateCostWithoutForwarding();
				} else {
					baseCost = new ModelCostCalculator(Main.loadDFD(model, model+"_0"), constraint, minCosts).calculateCost();
					satCost = new ModelCostCalculator(repairedDfd, constraint, minCosts).calculateCost();
				}
				satCost = satCost-baseCost;
				Config config = new Config(true, true, false, true, false);
				SolvingResult solvingResult = Main.run(Main.loadDFD(model, model + "_0"), constraintMap.get(variant), null, config);
				int smtCost = solvingResult.repairCost();
				System.out.println("Comparing " + model + "_" + variant);
				System.out.println("MCC Base Cost "+baseCost);
				System.out.println("MCC SAT Cost "+satCost);
				System.out.println("SMT Cost "+solvingResult.repairCost());
				solvingResult.repairActions().forEach(x -> System.out.println(x));
				if (Util.countViolations(solvingResult.repairedDFD(), constraintMap.get(variant)) > 0){
					System.out.println("Violatiosn present");
					System.exit(1);
				}
				if (model.equals("sqshq") && variant == 11) {
					System.exit(0);
				}
				ComparisonResult comparisonResult = new ComparisonResult(model, variant, satCost, smtCost);
				comparisonResults.add(comparisonResult);
			}
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		Path out = Path.of("testresults/results/modificationResults/comparison/data.json");

		Files.createDirectories(out.getParent());

		mapper.writeValue(out.toFile(), comparisonResults);

		System.out.println(comparisonResults);
	}

	private record ComparisonResult(String model, int constraints, int satCost, int smtCost) {
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

	private RepairResult runRepair(String model, String name, Boolean store, List<Constraint> constraints,
			Map<Label, Integer> costMap)
			throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
		var dfd = Main.loadDFD(model, name);
		if (!store)
			name = "aName";
		Mechanic mechanic = new Mechanic(dfd, name, constraints, costMap);
		long startTime = System.currentTimeMillis();
		var repairedDfd = mechanic.repair();
		long endTime = System.currentTimeMillis();
		int violationsAfter = new Mechanic(repairedDfd, null, null).amountOfViolations(repairedDfd, constraints);
		return new RepairResult(repairedDfd, mechanic.getViolations(), violationsAfter, endTime - startTime);
	}

	private record RepairResult(DataFlowDiagramAndDictionary repairedDfd, int violationsBefore, int violationsAfter,
			long runtimeInMilliseconds) {
	}

	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

}
