package dev.arcovia.mitigation.smt.tests.constraints;

import java.util.List;
import java.util.stream.Stream;

import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class VertexCharacteristicsListTest extends AbstractSelectorConstraintTest{

	@Override
	protected Stream<SelectorTestCase> cases() {
		return vertexCharacteristicsListCases();
	}
	
	static Stream<SelectorTestCase> vertexCharacteristicsListCases() {
		  return Stream.of( 
				 new SelectorTestCase(
		          "Source needs to remove label",
		          new ConstraintDSL()
		              .fromNode()
		              .withCharacteristic("dummyType", List.of("dummyLabel1"))
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("(not source_label_dummyLabel1)"),
		          List.of("true")
		      ),
				 new SelectorTestCase(
				          "Sink needs to remove label",
				          new ConstraintDSL()
				              .fromNode()
				              .withCharacteristic("dummyType", List.of("dummyLabel2"))
				              .neverFlows()
				              .toVertex()
				              .create(),
				          List.of("true"),
							List.of("(not sink_label_dummyLabel2)")
				      ),
				 new SelectorTestCase(
							"Sink needs to remove label 2",
				          new ConstraintDSL()
				              .fromNode()
									.withCharacteristic("dummyType", List.of("dummyLabel3", "dummyLabel2"))
				              .neverFlows()
				              .toVertex()
				              .create(),
				          List.of("true"),
							List.of("(not sink_label_dummyLabel2)")
					),
					new SelectorTestCase("Both need ot remove their label",
							new ConstraintDSL().fromNode()
									.withCharacteristic("dummyType", List.of("dummyLabel1", "dummyLabel2")).neverFlows()
									.toVertex().create(),
							List.of("(not source_label_dummyLabel1)"),
							List.of("(not sink_label_dummyLabel2)")
					),
					new SelectorTestCase("Empty list always satisfied for normal selector",
							new ConstraintDSL().fromNode()
									.withCharacteristic("dummyType", List.of()).neverFlows()
									.toVertex().create(),
							List.of("true"), List.of("true")),
					new SelectorTestCase("Both need to add the label",
							new ConstraintDSL().fromNode().withoutCharacteristic("dummyType", List.of("dummyLabel3"))
									.neverFlows().toVertex().create(),
							List.of("source_label_dummyLabel3"),
							List.of("sink_label_dummyLabel3")),
					new SelectorTestCase("Empty list never satisfied for inverted",
							new ConstraintDSL().fromNode()
									.withoutCharacteristic("dummyType", List.of()).neverFlows()
									.toVertex().create(),
							List.of("false"), List.of("false")),
					 new SelectorTestCase(
					          "Source needs to remove label",
					          new ConstraintDSL()
					              .fromNode()
					              .neverFlows()
					              .toVertex()
					              .withCharacteristic("dummyType", List.of("dummyLabel1"))
					              .create(),
					          List.of("(not source_label_dummyLabel1)"),
					          List.of("true")
					      ),
							 new SelectorTestCase(
							          "Sink needs to remove label",
							          new ConstraintDSL()
							              .fromNode()
							              .neverFlows()
							              .toVertex()
							              .withCharacteristic("dummyType", List.of("dummyLabel2"))
							              .create(),
							          List.of("true"),
										List.of("(not sink_label_dummyLabel2)")
							      ),
							 new SelectorTestCase(
										"Sink needs to remove label 2",
							          new ConstraintDSL()
							              .fromNode()
							              .neverFlows()
							              .toVertex()
											.withCharacteristic("dummyType", List.of("dummyLabel3", "dummyLabel2"))
							              .create(),
							          List.of("true"),
										List.of("(not sink_label_dummyLabel2)")
								),
								new SelectorTestCase("Both need ot remove their label",
										new ConstraintDSL().fromNode()
												.neverFlows()
												.toVertex()
												.withCharacteristic("dummyType", List.of("dummyLabel1", "dummyLabel2")).create(),
										List.of("(not source_label_dummyLabel1)"),
										List.of("(not sink_label_dummyLabel2)")
								),
								new SelectorTestCase("Empty list always satisfied for normal selector",
										new ConstraintDSL().fromNode()
												.neverFlows()
												.toVertex().withCharacteristic("dummyType", List.of()).create(),
										List.of("true"), List.of("true")),
								new SelectorTestCase("Both need to add the label",
										new ConstraintDSL().fromNode()
												.neverFlows().toVertex().withoutCharacteristic("dummyType", List.of("dummyLabel3")).create(),
										List.of("source_label_dummyLabel3"),
										List.of("sink_label_dummyLabel3")),
								new SelectorTestCase("Empty list never satisfied for inverted",
										new ConstraintDSL().fromNode()
											.neverFlows()
												.toVertex().withoutCharacteristic("dummyType", List.of()).create(),
										List.of("false"), List.of("false"))

		   );
		}
}
