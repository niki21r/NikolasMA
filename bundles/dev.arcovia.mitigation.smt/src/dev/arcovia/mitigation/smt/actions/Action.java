package dev.arcovia.mitigation.smt.actions;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;

/**
 * @author Nikolas Rank
 * Represents an Action that can be executed on a DFD to modify it.
 */
public interface Action {
	
	/**
	 * Applies this action to the DFD
	 * @param dfd incoming dfd
	 * @return resulting dfd
	 */
	abstract DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd);
	
	/**
	 * Reverts the changes of this action on the DFD
	 * @param dfd incoming dfd
	 * @return resulting dfd
	 */
	abstract DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd);
}
