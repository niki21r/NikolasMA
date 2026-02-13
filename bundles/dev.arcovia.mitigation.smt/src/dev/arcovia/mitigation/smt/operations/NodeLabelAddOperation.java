package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class NodeLabelAddOperation extends AbstractNodeLabelOperation {

	public NodeLabelAddOperation(Node node, Label label) {
		super(node, label);
	}

	@Override
	public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
		return super.doAction(dfd);
	}

	@Override
	public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
		return super.undoAction(dfd);
	}

	@Override
	public String toString() {
		return "Add "+label.getEntityName()+ " to Node "+node.getId()+" "+node.getEntityName();
	}

}
