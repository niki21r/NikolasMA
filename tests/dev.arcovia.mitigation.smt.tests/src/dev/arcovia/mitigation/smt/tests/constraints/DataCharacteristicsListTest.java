package dev.arcovia.mitigation.smt.tests.constraints;
import java.util.List;
import java.util.stream.Stream;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public class DataCharacteristicsListTest extends AbstractSelectorConstraintTest{


	@Override
	protected Stream<SelectorTestCase> cases() {
		return dataCharacteristicsListCases();
	}
	
	
	static Stream<SelectorTestCase> dataCharacteristicsListCases() {
		  return Stream.of(
		      // Sink constraint should only be satisfied when the label is removed.
		      new SelectorTestCase(
		          "withLabel(single) -> sink satisfies only if label removed",
		          new ConstraintDSL()
		              .ofData()
		              .withLabel("dummyType", List.of("dummyLabel1"))
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of("Pin_pin1_unset_dummyLabel1")
		      ),

		      // Sink constraint should only be satisfied when both labels are removed.
		      new SelectorTestCase(
		          "withLabel(multi) -> sink satisfies only if both labels removed",
		          new ConstraintDSL()
		              .ofData()
		              .withLabel("dummyType", List.of("dummyLabel1", "dummyLabel2"))
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of(
		              "(not (or (not Pin_pin1_unset_dummyLabel1) (not Pin_pin1_unset_dummyLabel2)))",
		              "(not (or (not Pin_pin1_unset_dummyLabel2) (not Pin_pin1_unset_dummyLabel1)))"
		          )
		      ),

		      // Edge case: Sink should always satisfy constraint with empty list.
		      new SelectorTestCase(
		          "withLabel(empty) -> sink always satisfies",
		          new ConstraintDSL()
		              .ofData()
		              .withLabel("dummyType", List.of())
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of("true")
		      ),

		      // Edge case: Sink should never satisfy negated selector with empty list.
		      new SelectorTestCase(
		          "withoutLabel(empty) -> sink never satisfies",
		          new ConstraintDSL()
		              .ofData()
		              .withoutLabel("dummyType", List.of())
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of("false")
		      ),

		      // Sink should satisfy this constraint since both labels are already present.
		      new SelectorTestCase(
		          "withoutLabel(dummyLabel1,dummyLabel2) -> sink satisfies (already present)",
		          new ConstraintDSL()
		              .ofData()
		              .withoutLabel("dummyType", List.of("dummyLabel1", "dummyLabel2"))
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of("true")
		      ),

		      // Sink should only satisfy if at least one of the labels is added.
		      new SelectorTestCase(
		          "withoutLabel(dummyLabel3,dummyLabel4) -> sink satisfies if one gets added",
		          new ConstraintDSL()
		              .ofData()
		              .withoutLabel("dummyType", List.of("dummyLabel3", "dummyLabel4"))
		              .neverFlows()
		              .toVertex()
		              .create(),
		          List.of("true"),
		          List.of(
		              "(or Pin_pin1_set_dummyLabel4 Pin_pin1_set_dummyLabel3)",
		              "(or Pin_pin1_set_dummyLabel3 Pin_pin1_set_dummyLabel4)"
		          )
		      )
		  );
		}
	}