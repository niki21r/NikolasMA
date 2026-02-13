package dev.arcovia.mitigation.smt.tests;

import static java.util.Map.entry;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

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

import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.util.Util;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import dev.arcovia.mitigation.sat.Constraint;
import dev.arcovia.mitigation.sat.Label;
import dev.arcovia.mitigation.sat.Mechanic;
import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.sat.dsl.CNFTranslation;

public class ScalabilityTest {

	private static final int RUNS_PER_CONFIGURATION = 10;
	private static final long MAX_TIME_MILLIS = TimeUnit.MINUTES.toMillis(10);
	private static final boolean checkViolationFree = false;

	private record ScaleInput(DataFlowDiagramAndDictionary inputDfd, List<AnalysisConstraint> inputConstraints,
			int scaleFactor) {
	};

	private record ScaleOutput(DataFlowDiagramAndDictionary outputDfd, List<AnalysisConstraint> outputConstraints) {
	};

	// SCALERS
	private static final Function<ScaleInput, ScaleOutput> scaleNodesAndFlows = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		return new ScaleOutput(scaler.scaleDFD(scaleInput.scaleFactor, scaleInput.scaleFactor),
				scaleInput.inputConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleTFGLength = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		int scaleFactor = powerOfTwo(scaleInput.scaleFactor);
		return new ScaleOutput(scaler.scaleTFGLength(scaleFactor), scaleInput.inputConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleTFGAmount = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		return new ScaleOutput(scaler.scaleTFGAmount(scaleInput.scaleFactor), scaleInput.inputConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleLabelTypes = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		int scaleFactor = powerOfTwo(scaleInput.scaleFactor);
		return new ScaleOutput(scaler.scaleLabelTypes(scaleFactor), scaleInput.inputConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleLabels = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		int scaleFactor = powerOfTwo(scaleInput.scaleFactor);
		return new ScaleOutput(scaler.scaleLabels(scaleFactor), scaleInput.inputConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintBeforeNeverFlows = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(scaleInput.scaleFactor * 4);
		List<AnalysisConstraint> allConstraints = new ArrayList<>();
		allConstraints.addAll(scaleInput.inputConstraints);
		allConstraints.addAll(scaler.scaleConstraint(1, scaleInput.scaleFactor, scaleInput.scaleFactor, 1, 1,
				scaleInput.scaleFactor * 4));
		return new ScaleOutput(dfdWithLabels, allConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintAfterNeverFlows = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(scaleInput.scaleFactor * 4);
		List<AnalysisConstraint> allConstraints = new ArrayList<>();
		allConstraints.addAll(scaleInput.inputConstraints);
		allConstraints.addAll(scaler.scaleConstraint(1, 1, 1, scaleInput.scaleFactor, scaleInput.scaleFactor,
				scaleInput.scaleFactor * 4));
		return new ScaleOutput(dfdWithLabels, allConstraints);
	};

	private static final Function<ScaleInput, ScaleOutput> scaleNumConstraints = (scaleInput) -> {
		Scaler scaler = new Scaler(scaleInput.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(scaleInput.scaleFactor * 4);
		List<AnalysisConstraint> allConstraints = new ArrayList<>();
		allConstraints.addAll(scaleInput.inputConstraints);
		allConstraints.addAll(scaler.scaleConstraint(scaleInput.scaleFactor, 1, 1, 1, 1, scaleInput.scaleFactor * 4));
		return new ScaleOutput(dfdWithLabels, allConstraints);
	};

	private static int powerOfTwo(int power) {
		return Integer.parseInt(BigInteger.TWO.pow(power).toString());
	}

	// Measure runtime of all TUHH Models with defined scaler and increasing scale
	// Factor until either tool exceeds runtime of 1h.
	public void scalabilityTest(Function<ScaleInput, ScaleOutput> scaleFunc, String name) throws Exception {
		try {

			var tuhhModels = TuhhModels.getTuhhModels();

			List<ScalabilityResult> scalabilityResults = new ArrayList<ScalabilityResult>();
			Map<Integer, List<AnalysisConstraint>> constraintMap = ConstraintMapProvider.buildConstraintMap();

			long runtimeSmtCurrRun = 0;
			long runtimeSatCurrRun = 0;
			int scale = 1;
			
			scaleLoop:
			do {
				List<Long> smtRuntimes = new ArrayList<>();
				List<Long> satRuntimes = new ArrayList<>();
				for (String model : tuhhModels.keySet()) {
					if (!tuhhModels.get(model).contains(0))
						continue;
					for (int i : List.of(1, 2, 4, 5, 7, 8, 10, 11)) {
						if (constraintMap.get(i) == null) {
							System.out.println(
									"Skipping " + model + " with constraint " + i + " because Constraint is undefined");
							continue;
						} else if (!tuhhModels.get(model).contains(i)) {
							System.out.println("Skipping " + model + " with constraint " + i
									+ " because no model for this constraint is defined");
							continue;
						}

						for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
							List<AnalysisConstraint> constraint = constraintMap.get(i);
							DataFlowDiagramAndDictionary smtDfd = Util.loadDFD(model, model + "_0");
							ScaleInput scaleInput = new ScaleInput(smtDfd, constraint, scale);
							ScaleOutput scaleOutput = scaleFunc.apply(scaleInput);
							long before = System.currentTimeMillis();
							SolvingResult solvingResult = Mitigation.run(scaleOutput.outputDfd, scaleOutput.outputConstraints,
									null);
							long after = System.currentTimeMillis();

							if (checkViolationFree) {
								if (j == 0 && (!solvingResult.satisfiable()
										|| Util.countViolations(solvingResult.repairedDFD(), constraint) > 0)) {
									System.out.println("Found error in SMT");
									scaleOutput.outputConstraints.forEach(System.out::println);
									System.exit(1);
								}
							}

							long smtRuntime = after - before;
							runtimeSmtCurrRun += smtRuntime;
							if (runtimeSmtCurrRun > MAX_TIME_MILLIS) {
								break scaleLoop;
							}
							smtRuntimes.add(smtRuntime);
						}

						for (int j = 0; j < RUNS_PER_CONFIGURATION; j++) {
							List<AnalysisConstraint> constraint = constraintMap.get(i);
							DataFlowDiagramAndDictionary smtDfd = Util.loadDFD(model, model + "_0");
							ScaleInput scaleInput = new ScaleInput(smtDfd, constraint, scale);
							ScaleOutput scaleOutput = scaleFunc.apply(scaleInput);
							RepairResult repairResult = runRepair(scaleOutput.outputDfd, false,
									scaleOutput.outputConstraints, minCosts);

							if (checkViolationFree) {
								if (j == 0 && (repairResult.violationsAfter() > 0
										|| Util.countViolations(repairResult.repairedDfd, constraint) > 0)) {
									System.out.println("Found error in SAT");
									scaleOutput.outputConstraints.forEach(System.out::println);
									System.exit(1);
								}
							}

							long satRuntime = repairResult.runtimeInMilliseconds;
							satRuntimes.add(satRuntime);
							runtimeSatCurrRun += satRuntime;
							if (runtimeSatCurrRun > MAX_TIME_MILLIS) {
								break scaleLoop;
							}
						}
					}
				}
				ScalabilityResult runtimeResult = new ScalabilityResult(scale, RUNS_PER_CONFIGURATION,
						runtimeSmtCurrRun, runtimeSatCurrRun, smtRuntimes, satRuntimes);
				scalabilityResults.add(runtimeResult);
				System.out.println("Scaled " + name + " by factor " + scale);
				System.out.println("Total Runtime SMT " + runtimeSmtCurrRun);
				System.out.println("Total Runtime SAT " + runtimeSatCurrRun);
				scale++;
				Thread.sleep(2500);
			} while (runtimeSmtCurrRun < MAX_TIME_MILLIS && runtimeSatCurrRun < MAX_TIME_MILLIS);

			System.out.println("Done");

			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			Path out = Path.of("testresults/results/scalabilityResults/" + name + "/" + RUNS_PER_CONFIGURATION + "Runs"
					+ (MAX_TIME_MILLIS / 60000) + "Minutes" + "/data.json");

			Files.createDirectories(out.getParent());

			mapper.writeValue(out.toFile(), scalabilityResults);
		} catch (

		Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

	@Test
	public void testTFGAmount() throws Exception {
		scalabilityTest(scaleTFGAmount, "tfgAmount");
	}

	@Test
	public void testTFGLength() throws Exception {
		scalabilityTest(scaleTFGLength, "tfgLength");
	}

	@Test
	public void testLabelTypes() throws Exception {
		scalabilityTest(scaleLabelTypes, "labelTypes");
	}
	
	@Test
	public void testLabels() throws Exception {
		scalabilityTest(scaleLabels, "labels");
	}

	@Test
	public void testLabelsInConstraintBeforeNeverFlows() throws Exception {
		scalabilityTest(scaleLabelsInConstraintBeforeNeverFlows, "labelsBeforeNeverFlows");
	}

	@Test
	public void testLabelsInConstraintAfterNeverFlows() throws Exception {
		scalabilityTest(scaleLabelsInConstraintAfterNeverFlows, "labelsAfterNeverFlows");
	}

	@Test
	public void testNumConstraints() throws Exception {
		scalabilityTest(scaleNumConstraints, "numConstraints");
	}

	@Test
	public void testNodesAndFlows() throws Exception {
		scalabilityTest(scaleNodesAndFlows, "nodesAndFlows");
	}

	@Test
	public void testAllForScalability() throws Exception {
		scalabilityTest(scaleTFGAmount, "tfgAmount");
		scalabilityTest(scaleTFGLength, "tfgLength");
		scalabilityTest(scaleLabelTypes, "labelTypes");
		scalabilityTest(scaleLabels, "labels");
		scalabilityTest(scaleLabelsInConstraintBeforeNeverFlows, "labelsBeforeNeverFlows");
		scalabilityTest(scaleLabelsInConstraintAfterNeverFlows, "labelsAfterNeverFlows");
		scalabilityTest(scaleNumConstraints, "numConstraints");
		scalabilityTest(scaleNodesAndFlows, "nodesAndFlows");
	}

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

	private record ScalabilityResult(int scale, int runsPerConfiguration, long totalRuntimeSMT, long totalRuntimeSAT,
			List<Long> runtimesSMT, List<Long> runtimesSAT) {
	}

	final Map<Label, Integer> minCosts = Map.ofEntries(entry(new Label("Stereotype", "gateway"), 1),
			entry(new Label("Stereotype", "authenticated_request"), 1),
			entry(new Label("Stereotype", "transform_identity_representation"), 1),
			entry(new Label("Stereotype", "token_validation"), 1),
			entry(new Label("Stereotype", "login_attempts_regulation"), 1),
			entry(new Label("Stereotype", "encrypted_connection"), 1),
			entry(new Label("Stereotype", "log_sanitization"), 1), entry(new Label("Stereotype", "local_logging"), 1));

}
