package dev.arcovia.mitigation.smt.actions;

import java.util.List;
import java.util.Optional;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;


public sealed abstract class AbstractPinAssignmentAction<T extends AbstractAssignment>
        extends DataDictionaryAction
        permits SetAssignmentAction, UnsetAssignmentAction {

    protected final Pin pin;
    protected final Label label;

    protected AbstractPinAssignmentAction(Pin pin, Label label) {
        this.pin = pin;
        this.label = label;
    }

    protected abstract T createAssignment();
    protected abstract boolean isInstance(AbstractAssignment a);
    protected abstract T cast(AbstractAssignment a);

    protected abstract void addOutputLabel(T assignment, Label labels);

    protected abstract boolean outputLabelEquals(T assignment, Label labels);

    protected abstract String assignmentName();

    @Override
    public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
        T assignment = createAssignment();
        assignment.setOutputPin(pin);
        assignment.setId(String.valueOf(random.nextInt()));
        addOutputLabel(assignment, label);

        Optional<List<AbstractAssignment>> assignments = dfd.dataDictionary().getBehavior().stream()
                .filter(b -> b.getOutPin().contains(pin))
                .map(Behavior::getAssignment)
                .findAny();

        if (assignments.isEmpty()) {
            logger.debug("Couldnt't find Node behavior for pin " + pin.getId());
        } else {
            assignments.get().add(assignment);
        }
        return dfd;
    }

    @Override
    public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
        Optional<Behavior> behavior = dfd.dataDictionary().getBehavior().stream()
                .filter(b -> b.getOutPin().contains(pin))
                .findFirst();

        if (behavior.isEmpty()) {
            logger.debug("Couldn't find matching behavior for " + pin);
            return dfd;
        }

        Optional<T> found = behavior.get().getAssignment().stream()
                .filter(a -> a.getOutputPin().equals(pin))
                .filter(this::isInstance)
                .map(this::cast)
                .filter(a -> outputLabelEquals(a, label))
                .findAny();

        if (found.isEmpty()) {
            logger.debug("Couldn't find matching " + assignmentName()
                    + " for pin " + pin + " with Labels " + label);
        } else {
            behavior.get().getAssignment().removeIf(a -> a.equals(found.get()));
        }

        return dfd; // your original returned null; returning dfd is usually intended
    }
}
