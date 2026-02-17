package dev.arcovia.mitigation.smt.tests.evaluation;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.arcovia.mitigation.sat.Scaler;
import dev.arcovia.mitigation.smt.Mitigation;
import dev.arcovia.mitigation.smt.SolvingResult;
import dev.arcovia.mitigation.smt.tests.evaluation.SatHelper.RepairResult;
import dev.arcovia.mitigation.smt.util.Util;

/**
 * 
 * @author Nikolas Rank Compares the runtime of SMT to SAT when different
 *         scalability dimensions are considered
 *
 */
public class ScalabilityTest {

	private static final int RUNS_PER_CONFIGURATION = 10;
	private static final long MAX_TIME_MILLIS = TimeUnit.MINUTES.toMillis(60);

	private record ScaleInput(DataFlowDiagramAndDictionary inputDfd, List<AnalysisConstraint> inputConstraints,
			int scaleFactor) {
	}

	private record ScaleOutput(DataFlowDiagramAndDictionary outputDfd, List<AnalysisConstraint> outputConstraints) {
	}

	private static int powerOfTwo(int power) {
		return Integer.parseInt(BigInteger.TWO.pow(power).toString());
	}

	public void scalabilityTest(Function<ScaleInput, ScaleOutput> scaleFunc, String name) throws Exception {
		List<EvaluationSupport.Configuration> configs = EvaluationSupport.configurations();
		List<ScalabilityResult> results = new ArrayList<>();

		int scale = 1;

		long totalRuntimeSmt = 0;
		long totalRuntimeSat = 0;

		// Increase scale while both runtimes are below bound.
		while (totalRuntimeSmt < MAX_TIME_MILLIS && totalRuntimeSat < MAX_TIME_MILLIS) {
			List<Long> smtRuntimes = new ArrayList<>();
			List<Long> satRuntimes = new ArrayList<>();
			List<Long> findTFGsTimes = new ArrayList<>();

			totalRuntimeSmt = 0;
			totalRuntimeSat = 0;
			long totalTimeFindTFGs = 0;

			// For all TUHH-Model & Constraint Pairs
			for (EvaluationSupport.Configuration cfg : configs) {
				for (int runIdx = 0; runIdx < RUNS_PER_CONFIGURATION; runIdx++) {
					DataFlowDiagramAndDictionary base = Util.loadDFD(cfg.model(), cfg.model() + "_0");
					// Scale respective dimension
					ScaleOutput out = scaleFunc.apply(new ScaleInput(base, cfg.constraints(), scale));

					// Check smt Time
					long before = System.currentTimeMillis();
					SolvingResult solving = Mitigation.run(out.outputDfd, out.outputConstraints, null);
					long after = System.currentTimeMillis();

					// Check for violations only once
					if (runIdx == 0) {
						if (!solving.satisfiable()
								|| Util.countViolations(solving.repairedDFD(), cfg.constraints()) > 0) {
							throw new IllegalStateException(
									"SMT invalid at scale=" + scale + " for " + cfg.model() + "_" + cfg.variantId());
						}
					}
					// Save outputs
					long dt = after - before;
					smtRuntimes.add(dt);
					totalRuntimeSmt += dt;

					totalTimeFindTFGs += solving.findTFGsTimeMs();
					findTFGsTimes.add(solving.findTFGsTimeMs());
				}
			}

			for (EvaluationSupport.Configuration cfg : configs) {
				for (int runIdx = 0; runIdx < RUNS_PER_CONFIGURATION; runIdx++) {
					DataFlowDiagramAndDictionary base = Util.loadDFD(cfg.model(), cfg.model() + "_0");
					ScaleOutput out = scaleFunc.apply(new ScaleInput(base, cfg.constraints(), scale));

					// Run Sat repair
					RepairResult rr = SatHelper.runRepair(out.outputDfd, false, out.outputConstraints,
							SatHelper.MIN_COSTS);

					if (runIdx == 0) {
						if (rr.violationsAfter() > 0 || Util.countViolations(rr.repairedDfd(), cfg.constraints()) > 0) {
							throw new IllegalStateException(
									"SAT invalid at scale=" + scale + " for " + cfg.model() + "_" + cfg.variantId());
						}
					}

					// Save outputs
					long dt = rr.runtimeInMilliseconds();
					satRuntimes.add(dt);
					totalRuntimeSat += dt;
				}
			}
			results.add(new ScalabilityResult(scale, RUNS_PER_CONFIGURATION, totalRuntimeSmt, totalRuntimeSat,
					smtRuntimes, satRuntimes, totalTimeFindTFGs, findTFGsTimes));
			scale++;
		}

		Path out = Path.of("testresults/results/scalabilityResults/" + name + "/" + RUNS_PER_CONFIGURATION + "Runs"
				+ (MAX_TIME_MILLIS / 60000) + "Minutes" + "/data.json");

		EvaluationSupport.writeJson(out, results);
	}

	@Test
	public void testTFGAmount() throws Exception {
		scalabilityTest(scaleTFGAmount, "tfgAmount");
	}

	// Scale TFG amount linearly
	private static final Function<ScaleInput, ScaleOutput> scaleTFGAmount = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		return new ScaleOutput(scaler.scaleTFGAmount(in.scaleFactor), in.inputConstraints);
	};

	@Test
	public void testTFGLength() throws Exception {
		scalabilityTest(scaleTFGLength, "tfgLength");
	}

	// Scale TFG length exponentially because effect is low
	private static final Function<ScaleInput, ScaleOutput> scaleTFGLength = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		int sf = powerOfTwo(in.scaleFactor);
		return new ScaleOutput(scaler.scaleTFGLength(sf), in.inputConstraints);
	};

	@Test
	public void testLabelTypes() throws Exception {
		scalabilityTest(scaleLabelTypes, "labelTypes");
	}

	// Scale label types exponentially because effect is low
	private static final Function<ScaleInput, ScaleOutput> scaleLabelTypes = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		int sf = powerOfTwo(in.scaleFactor);
		return new ScaleOutput(scaler.scaleLabelTypes(sf), in.inputConstraints);
	};

	@Test
	public void testLabels() throws Exception {
		scalabilityTest(scaleLabels, "labels");
	}

	// Scale labels exponentially because effect is low
	private static final Function<ScaleInput, ScaleOutput> scaleLabels = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		int sf = powerOfTwo(in.scaleFactor);
		return new ScaleOutput(scaler.scaleLabels(sf), in.inputConstraints);
	};

	@Test
	public void testLabelsInConstraintBeforeNeverFlows() throws Exception {
		scalabilityTest(scaleLabelsInConstraintBeforeNeverFlows, "labelsBeforeNeverFlows");
	}

	// Scale single constraint with increasing amount of data selectors linearly.
	// Include the existing constraints for config as well.
	private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintBeforeNeverFlows = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(in.scaleFactor);

		List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
		all.addAll(scaler.scaleConstraint(1, in.scaleFactor, in.scaleFactor, 0, 0, in.scaleFactor));

		return new ScaleOutput(dfdWithLabels, all);

	};

	@Test
	public void testLabelsInConstraintAfterNeverFlows() throws Exception {
		scalabilityTest(scaleLabelsInConstraintAfterNeverFlows, "labelsAfterNeverFlows");
	}

	// Scale single constraint with increasing amount of vertex selectors linearly.
	// Include the existing constraints for config as well.
	private static final Function<ScaleInput, ScaleOutput> scaleLabelsInConstraintAfterNeverFlows = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(in.scaleFactor);

		List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
		all.addAll(scaler.scaleConstraint(1, 0, 0, in.scaleFactor, in.scaleFactor, in.scaleFactor));
		System.out.println(all);
		return new ScaleOutput(dfdWithLabels, all);
	};

	@Test
	public void testNumConstraints() throws Exception {
		scalabilityTest(scaleNumConstraints, "numConstraints");
	}

	// Scale number of constraints of form data X && !Y neverFlows vertex V && !Z
	// linearly. Include the existing constraints for config as well
	private static final Function<ScaleInput, ScaleOutput> scaleNumConstraints = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		DataFlowDiagramAndDictionary dfdWithLabels = scaler.scaleLabels(in.scaleFactor * 4);

		List<AnalysisConstraint> all = new ArrayList<>(in.inputConstraints);
		all.addAll(scaler.scaleConstraint(in.scaleFactor, 1, 1, 1, 1, in.scaleFactor * 4));

		return new ScaleOutput(dfdWithLabels, all);
	};

	// Not used because redundant
	@Disabled
	@Test
	public void testNodesAndFlows() throws Exception {
		scalabilityTest(scaleNodesAndFlows, "nodesAndFlows");
	}

	// Scale nodes and flows linearly.
	private static final Function<ScaleInput, ScaleOutput> scaleNodesAndFlows = (in) -> {
		Scaler scaler = new Scaler(in.inputDfd);
		return new ScaleOutput(scaler.scaleDFD(in.scaleFactor, in.scaleFactor), in.inputConstraints);
	};

	private record ScalabilityResult(int scale, int runsPerConfiguration, long totalRuntimeSMT, long totalRuntimeSAT,
			List<Long> runtimesSMT, List<Long> runtimesSAT, long totalTimeFindTFGs, List<Long> findTFGsTime) {
	}
}
