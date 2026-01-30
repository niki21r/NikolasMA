package dev.arcovia.mitigation.smt.actions;

import java.util.List;

import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.SetAssignment;


public final class SetAssignmentAction extends AbstractPinAssignmentAction<SetAssignment> {

    public SetAssignmentAction(Pin pin, Label label) { super(pin, label); }

    @Override protected SetAssignment createAssignment() { return factory.createSetAssignment(); }
    @Override protected boolean isInstance(AbstractAssignment a) { return a instanceof SetAssignment; }
    @Override protected SetAssignment cast(AbstractAssignment a) { return (SetAssignment) a; }

    @Override
    protected void addOutputLabel(SetAssignment assignment, Label label) {
        assignment.getOutputLabels().add(label);
    }

    @Override
    protected boolean outputLabelEquals(SetAssignment assignment, Label label) {
        return assignment.getOutputLabels().equals(List.of(label));
    }

    @Override protected String assignmentName() { return "Set Assignment"; }
    
    @Override public String toString() {
    	return "Set at Pin "+pin.getId()+" with Label "+label.getEntityName();
    }
}


