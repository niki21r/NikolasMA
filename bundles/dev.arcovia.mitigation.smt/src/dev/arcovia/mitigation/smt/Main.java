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
public class Main {

	/**
	 * Repairs DFD. First it preprocesses, using existing DFA tooling, creates
	 * mappings of DFD entities to integers, and finally repairs
	 * 
	 * @param dfd         Input dataflow Diagram
	 * @param constraints Constraints that the output needs to adhere too
	 * @throws StandaloneInitializationException If input DFD is incorrect
	 */
	public static SolvingResult run(DataFlowDiagramAndDictionary dfd,
			List<AnalysisConstraint> constraints, Map<String, Integer> labelCosts, Config config) throws StandaloneInitializationException {
		Preprocess preprocces = new Preprocess();
		PreprocessingResult preprocessingResult = preprocces.preprocess(dfd, constraints);
		SMTMappings mappings = new SMTMappings(preprocessingResult);
		if (config == null) {
			config = new Config();
		}
		SMT smt = new SMT(preprocessingResult, constraints, mappings, labelCosts, config);
		return smt.repair();
	}

	public static long findDagSize(DataFlowDiagramAndDictionary dfd, List<AnalysisConstraint> constraints)
			throws StandaloneInitializationException {
		Preprocess preprocces = new Preprocess();
		PreprocessingResult preprocessingResult = preprocces.preprocess(dfd, constraints);
		SMTMappings mappings = new SMTMappings(preprocessingResult);
		SMT smt = new SMT(preprocessingResult, constraints, mappings, null, null);
		return smt.getDagSizeAfterSolving();
	}

	/**
	 * Loads a dfd. Currently only static from the hardcoded folder
	 * 
	 * @param model Model that the dfd resides in
	 * @param name  Filename without file endings
	 * @return Loaded dfd
	 * @throws StandaloneInitializationException If input DFD at paths is incorrect
	 *                                           or can not be properly resolved
	 */
	public static DataFlowDiagramAndDictionary loadDFD(String model, String name)
			throws StandaloneInitializationException {
		final String PROJECT_NAME = "org.dataflowanalysis.examplemodels";
		final String location = Paths.get("scenarios", "dfd", "TUHH-Models").toString();
		return new DataFlowDiagramAndDictionary(PROJECT_NAME,
				Paths.get(location, model, (name + ".dataflowdiagram")).toString(),
				Paths.get(location, model, (name + ".datadictionary")).toString(), Activator.class);
	}

}
