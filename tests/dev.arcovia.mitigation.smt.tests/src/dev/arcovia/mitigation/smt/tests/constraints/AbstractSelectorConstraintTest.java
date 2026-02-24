package dev.arcovia.mitigation.smt.tests.constraints;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;
import dev.arcovia.mitigation.smt.config.Config;
import dev.arcovia.mitigation.smt.config.ConfigBuilder;
import dev.arcovia.mitigation.smt.constraints.ConstraintTranslator;
import dev.arcovia.mitigation.smt.preprocess.Preprocess;
import dev.arcovia.mitigation.smt.preprocess.PreprocessingResult;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSelectorConstraintTest {

  protected abstract Stream<SelectorTestCase> cases();

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  public void selector_constraint_translates_as_expected(SelectorTestCase tc) {
    DataFlowDiagramAndDictionary dfdAndDD = DFDProvider.buildTestCase();

    AnalysisConstraint constraint = tc.constraint();
    List<AnalysisConstraint> constraints = new ArrayList<>();
    constraints.add(constraint);

    Preprocess preprocess = new Preprocess();
    PreprocessingResult pre = preprocess.preprocess(dfdAndDD, constraints, false);

    Config config = new ConfigBuilder().build();
    SMT smt = new SMT(pre, constraints, config);

    ConstraintTranslator translator = new ConstraintTranslator(smt);
    Set<DFDVertex> vertices = pre.vertices();

    Node source = dfdAndDD.dataFlowDiagram().getNodes().stream()
        .filter(x -> x.getEntityName().equals("source"))
        .findFirst().orElseThrow();

    DFDVertex sourceVertex = vertices.stream()
        .filter(x -> x.getReferencedElement().equals(source))
        .findFirst().orElseThrow();

    Node sink = dfdAndDD.dataFlowDiagram().getNodes().stream()
        .filter(x -> x.getEntityName().equals("sink"))
        .findFirst().orElseThrow();

    DFDVertex sinkVertex = vertices.stream()
        .filter(x -> x.getReferencedElement().equals(sink))
        .findFirst().orElseThrow();

    String sourceFormula = ((BoolExpr) translator.translateConstraint(constraint, sourceVertex).simplify()).toString();
    assertTrue(
        tc.validSourceFormulas().contains(sourceFormula),
			() -> "Unexpected source formula: " + sourceFormula.toString() + " \nExpected one of: "
					+ tc.validSourceFormulas() + "\nActual: " + sourceFormula
    );
    String sinkFormula = ((BoolExpr) translator.translateConstraint(constraint, sinkVertex).simplify()).toString();
    assertTrue(
        tc.validSinkFormulas().contains(sinkFormula),
			() -> "Unexpected sink formula: " + sinkFormula.toString() + "\nExpected one of: " + tc.validSinkFormulas()
					+ "\nActual: " + sinkFormula
	);
    
  }
}