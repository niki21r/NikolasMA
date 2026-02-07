package dev.arcovia.mitigation.smt.tests;

import static java.util.Map.entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Configurator;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.TuhhModels;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.arcovia.mitigation.smt.Main;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.util.Util;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

public class ScalabilityTest {

	private static final int RUNS_PER_CONFIGURATION = 5;
	
	@Test
	public void testAllForScalability() throws Exception {
		try {
			
			var tuhhModels = TuhhModels.getTuhhModels();

			List<ScalabilityResult> scalabilityResults = new ArrayList<ScalabilityResult>();
			Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();

			long totalRuntimeSMT = Long.MIN_VALUE;
			long totalRuntimeSAT = Long.MIN_VALUE;
			int scale = 0;
			
			BiFunction<DataFlowDiagramAndDictionary, Integer, DataFlowDiagramAndDictionary> scaleNodesAndFlows = (dfd, scaleFactor) -> {
				Scaler scaler = new Scaler(dfd);
				return scaler.scaleDFD(scaleFactor, scaleFactor);
			};
			
			BiFunction<DataFlowDiagramAndDictionary, Integer, DataFlowDiagramAndDictionary> scaleTFGLength = (dfd, scaleFactor) -> {
				Scaler scaler = new Scaler(dfd);
				return scaler.scaleTFGLength(scaleFactor);
			};
			
			BiFunction<DataFlowDiagramAndDictionary, Integer, DataFlowDiagramAndDictionary> scaleTFGAmount = (dfd, scaleFactor) -> {
				Scaler scaler = new Scaler(dfd);
				return scaler.scaleTFGAmount(scaleFactor);
			};
			
			BiFunction<DataFlowDiagramAndDictionary, Integer, DataFlowDiagramAndDictionary> scaleLabels = (dfd, scaleFactor) -> {
				Scaler scaler = new Scaler(dfd);
				return scaler.scaleLabels(scaleFactor);
			};
			
			BiFunction<DataFlowDiagramAndDictionary, Integer, DataFlowDiagramAndDictionary> scaleLabelsInConstraintsBeforeNeverFlow = (dfd, scaleFactor) -> {
				Scaler scaler = new Scaler(dfd);
				scaler.scaleLabels(scaleFactor);
				scaler.scaleConstraint(1, scaleFactor, scaleFactor, 1, 1, scaleFactor);
				return scaler.scaleTFGAmount(scaleFactor);
			};



			
			do {
				List<Long> smtRuntimes = new ArrayList<>();
				List<Long> satRuntimes = new ArrayList<>();
				for (String model : tuhhModels.keySet()) {
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

						for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
							DataFlowDiagramAndDictionary smtDfd = Main.loadDFD(model, model + "_0");
							Scaler smtScaler = new Scaler(smtDfd);
							smtDfd = smtScaler.scaleDFD(scale, scale);
							long before = System.currentTimeMillis();
							SolvingResult solvingResult = Main.run(smtDfd, constraint, null);
							long after = System.currentTimeMillis();
							/**
							if (!solvingResult.satisfiable()
									|| Util.countViolations(solvingResult.repairedDFD(), constraint) > 0) {
								System.out.println("Found error in SMT");
								System.exit(1);
							};
							*/
							long smtRuntime = after - before;
							smtRuntimes.add(smtRuntime);
						}
						
						for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
							DataFlowDiagramAndDictionary satDfd = Main.loadDFD(model, model + "_0");
							Scaler scaler = new Scaler(satDfd);
							satDfd = scaler.scaleDFD(scale, scale);
							RepairResult repairResult = runRepair(satDfd, false, constraint, minCosts);
							/**
							if (repairResult.violationsAfter() > 0
									|| Util.countViolations(repairResult.repairedDfd, constraint) > 0) {
								System.out.println("Found error in SAT");
								System.exit(1);
							}*/
							long satRuntime = repairResult.runtimeInMilliseconds;
							satRuntimes.add(satRuntime);
						}
					}
				}
				totalRuntimeSMT = smtRuntimes.stream()
						.mapToLong(Long::longValue).sum();
				totalRuntimeSAT = satRuntimes.stream()
						.mapToLong(Long::longValue).sum();
				ScalabilityResult runtimeResult = new ScalabilityResult(scale, totalRuntimeSMT, totalRuntimeSAT, smtRuntimes, satRuntimes);
				scalabilityResults.add(runtimeResult);
				System.out.println("Scale "+scale);
				System.out.println("Total Runtime SMT "+totalRuntimeSMT);
				System.out.println("Total Runtime SAT "+totalRuntimeSAT);
				scale++;
				Thread.sleep(2500);
			} while (totalRuntimeSMT < 60000 && totalRuntimeSAT < 60000);

			System.out.println("Done");
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/scalabilityResults/" + RUNS_PER_CONFIGURATION + "/scaleTFGs/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), scalabilityResults);
		} catch (

		Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
	/*
	 *
	 * DataFlowDiagramAndDictionary dfd = Main.loadDFD(model, model + "_0"); Scaler
	 * scaler = new Scaler(dfd); //TFG length scaler.scaleTFGLength(1); //Tfg amount
	 * scaler.scaleTFGAmount(1); //Labels scaler.scaleLabelTypes(1);
	 * scaler.scaleLabels(1); //Labels und Label typen vor neverflows
	 * scaler.scaleLabelTypes(i); scaler.scaleLabels(i); scaler.scaleConstraint(1,
	 * i, i, 1, 1, i); //Labels und Label Typen nach Neverflows
	 * scaler.scaleLabelTypes(i); scaler.scaleLabels(i); scaler.scaleConstraint(1,
	 * 1, 1, i, i, i); // scaler.scaleLabelTypes(1); scaler.scaleLabels(1);
	 * scaler.scaleConstraint(i, 1, 1, 1, 1, 1);
	 */

	private RepairResult runRepair(DataFlowDiagramAndDictionary dfd, Boolean store,
			List<AnalysisConstraint> analysisConstraints, Map<Label, Integer> costMap)
			throws StandaloneInitializationException, ContradictionException, IOException, TimeoutException {
		String name = dfd.dataFlowDiagram().getEntityName();
		if (!store)
			name = "aName";
		long startTime = System.currentTimeMillis();
		List<Constraint> translated = analysisConstraints.stream()
				.flatMap(x -> new CNFTranslation(x).constructCNF().stream()).toList();
		Mechanic mechanic = new Mechanic(dfd, name, translated, costMap);
		var repairedDfd = mechanic.repair();
		long endTime = System.currentTimeMillis();
		int violationsAfter = new Mechanic(repairedDfd, null, null).amountOfViolations(repairedDfd, translated);
		return new RepairResult(repairedDfd, mechanic.getViolations(), violationsAfter, endTime - startTime);
	}

	private record RepairResult(DataFlowDiagramAndDictionary repairedDfd, int violationsBefore, int violationsAfter,
			long runtimeInMilliseconds) {
	}

	private record ScalabilityResult(int scale, long totalRuntimeSMT, long totalRuntimeSAT, List<Long> runtimesSMT, List<Long> runtimesSAT) {
	}

	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

}
