package dev.arcovia.mitigation.smt.actions;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class NodeLabelRemoveAction extends AbstractNodeLabelAction {

	public NodeLabelRemoveAction(Node node, Label label) {
		super(node, label);
	}
	
	@Override
	public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
		return super.undoAction(dfd);
	}

	@Override
	public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
		return super.doAction(dfd);
	}

	@Override
	public String toString() {
		return "Remove "+label.getEntityName()+ " from Node "+node.getId()+" "+node.getEntityName();
	}
}
