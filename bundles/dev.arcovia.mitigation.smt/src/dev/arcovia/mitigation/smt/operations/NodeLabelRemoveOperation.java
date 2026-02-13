package dev.arcovia.mitigation.smt.operations;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class NodeLabelRemoveOperation extends AbstractNodeLabelOperation {

	public NodeLabelRemoveOperation(Node node, Label label) {
		super(node, label);
	}
	
	@Override
	public DataFlowDiagramAndDictionary doOperation(DataFlowDiagramAndDictionary dfd) {
		return super.undoOperation(dfd);
	}

	@Override
	public DataFlowDiagramAndDictionary undoOperation(DataFlowDiagramAndDictionary dfd) {
		return super.doOperation(dfd);
	}

	@Override
	public String toString() {
		return "Remove "+label.getEntityName()+ " from Node "+node.getId()+" "+node.getEntityName();
	}
}
