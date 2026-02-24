package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexCharacteristicsTest extends AbstractSelectorConstraintTest{

	@Override
	protected Stream<SelectorTestCase> cases() {
		return vertexCharacteristicsCases();
	}
	
	static Stream<SelectorTestCase> vertexCharacteristicsCases() {
		  return Stream.of( 
				 new SelectorTestCase(
		          "Source needs to remove label",
		          new ConstraintDSL()
		              .fromNode()
		              .withCharacteristic("dummyType", "dummyLabel1")
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("(not source_label_dummyLabel1)"),
		          List.of("(not source_label_dummyLabel1)")
		      ), 
				 new SelectorTestCase(
				          "Only sink needs to remove label",
				          new ConstraintDSL()
				              .fromNode()
				              .withCharacteristic("dummyType", "dummyLabel2")
				              .neverFlows()
				              .toVertex()
				              .create(),
				          List.of("true"),
				          List.of("(not sink_label_dummyLabel2)")),
				 new SelectorTestCase(
				          "Only source needs to remove label",
				          new ConstraintDSL()
				              .fromNode()
				              .neverFlows()
				              .toVertex()
				              .withCharacteristic("dummyType", "dummyLabel1")
				              .create(),
				          List.of("(not source_label_dummyLabel1)"),
				          List.of("true")),
				 new SelectorTestCase(
				          "No match",
				          new ConstraintDSL()
				              .fromNode()
				              .neverFlows()
				              .toVertex()
				              .withCharacteristic("dummyType", "dummyLabel3")
				              .create(),
				          List.of("true"),
				          List.of("true"))
		   );
		}
}
