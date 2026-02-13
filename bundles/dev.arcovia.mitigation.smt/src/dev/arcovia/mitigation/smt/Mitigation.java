package dev.arcovia.mitigation.smt;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.examplemodels.Activator;

import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;

/**
 * @author Nikolas Rank This class provides a static entrypoint into the solver.
 *         Given a DFD, and a list of constraints it returns a repaired DFD.
 *
 */
public class Mitigation {

	/**
	 * Repairs DFD. First it preprocesses, using existing DFA tooling, creates
	 * mappings of DFD entities to integers, and finally repairs
	 * 
	 * @param dfd         Input dataflow Diagram
	 * @param constraints Constraints that the output needs to adhere too
	 * @throws StandaloneInitializationException If input DFD is incorrect
	 */
	public static SolvingResult run(DataFlowDiagramAndDictionary dfd,
			List<AnalysisConstraint> constraints, Config config) throws StandaloneInitializationException {
		Preprocess preprocces = new Preprocess();
		PreprocessingResult preprocessingResult = preprocces.preprocess(dfd, constraints);
		SMTMappings mappings = new SMTMappings(preprocessingResult);
		if (config == null) {
			config = new Config();
		}
		SMT smt = new SMT(preprocessingResult, constraints, mappings, config);
		return smt.repair();
	}
}
