package dev.arcovia.mitigation.smt.actions;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public abstract class AbstractNodeLabelAction extends DataDictionaryAction{

	protected Node node;
	protected Label label;
	
	public AbstractNodeLabelAction(Node node, Label label) {
		this.node = node;
		this.label = label;
	}
	
	@Override
	public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
		dfd.dataFlowDiagram().getNodes().stream().filter(x -> x.equals(node)).forEach(x -> x.getProperties().add(label));
		return dfd;
	}

	@Override
	public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
		dfd.dataFlowDiagram().getNodes().stream().filter(x -> x.equals(node)).forEach(x -> x.getProperties().remove(label));
		return dfd;
	}

}
