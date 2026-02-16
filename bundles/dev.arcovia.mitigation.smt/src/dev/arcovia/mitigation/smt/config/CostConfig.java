package dev.arcovia.mitigation.smt.config;

import java.util.HashMap;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class CostConfig {

	public HashMap<String, Integer> getAddLabelCost() {
		return addLabelCost;
	}

	public HashMap<String, Integer> getRemoveLabelCost() {
		return removeLabelCost;
	}

	public HashMap<Node, Integer> getNodeFactor() {
		return nodeFactor;
	}

	public HashMap<Pin, Integer> getPinFactor() {
		return pinFactor;
	}

	public boolean isWeighTFGs() {
		return weighTFGs;
	}

	public void setNodeFactor(HashMap<Node, Integer> nodeFactor) {
		this.nodeFactor = nodeFactor;
	}

	public void setPinFactor(HashMap<Pin, Integer> pinFactor) {
		this.pinFactor = pinFactor;
	}

	private final HashMap<String, Integer> addLabelCost;
	private final HashMap<String, Integer> removeLabelCost;
	private HashMap<Node, Integer> nodeFactor;
	private HashMap<Pin, Integer> pinFactor;
	private final boolean weighTFGs;

	protected CostConfig(HashMap<String, Integer> addLabelCost, HashMap<String, Integer> removeLabelCost,
			HashMap<Node, Integer> nodeFactor, HashMap<Pin, Integer> pinFactor, boolean weighTFGs) {
		this.addLabelCost = addLabelCost;
		this.removeLabelCost = removeLabelCost;
		this.nodeFactor = nodeFactor;
		this.pinFactor = pinFactor;
		this.weighTFGs = weighTFGs;
	}
}
