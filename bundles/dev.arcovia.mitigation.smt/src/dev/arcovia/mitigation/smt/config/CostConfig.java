package dev.arcovia.mitigation.smt.config;

import java.util.Map;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class CostConfig {

	public Map<String, Integer> getAddLabelCost() {
		return addLabelCost;
	}

	public Map<String, Integer> getRemoveLabelCost() {
		return removeLabelCost;
	}

	public Map<Node, Integer> getNodeFactor() {
		return nodeFactor;
	}

	public Map<Pin, Integer> getPinFactor() {
		return pinFactor;
	}

	public boolean isWeighTFGs() {
		return weighTFGs;
	}

	public void setNodeFactor(Map<Node, Integer> nodeFactor) {
		this.nodeFactor = nodeFactor;
	}

	public void setPinFactor(Map<Pin, Integer> pinFactor) {
		this.pinFactor = pinFactor;
	}

	private final Map<String, Integer> addLabelCost;
	private final Map<String, Integer> removeLabelCost;
	private Map<Node, Integer> nodeFactor;
	private Map<Pin, Integer> pinFactor;
	private final boolean weighTFGs;

	protected CostConfig(Map<String, Integer> addLabelCost, Map<String, Integer> removeLabelCost,
			Map<Node, Integer> nodeFactor, Map<Pin, Integer> pinFactor, boolean weighTFGs) {
		this.addLabelCost = addLabelCost;
		this.removeLabelCost = removeLabelCost;
		this.nodeFactor = nodeFactor;
		this.pinFactor = pinFactor;
		this.weighTFGs = weighTFGs;
	}
}
