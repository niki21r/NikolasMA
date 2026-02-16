package dev.arcovia.mitigation.smt.config;

import java.util.HashMap;
import java.util.Map;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class CostConfigBuilder {

	private Map<String, Integer> addLabelCost = new HashMap<>();
	private Map<String, Integer> removeLabelCost = new HashMap<>();
	private Map<Node, Integer> nodeFactor = new HashMap<>();
	private Map<Pin, Integer> pinFactor = new HashMap<>();
	private boolean weighTFGs = false;

	public CostConfigBuilder() {
	}

	public CostConfigBuilder withLabelCost(Map<String, Integer> labelCost) {
		addLabelCost = labelCost;
		removeLabelCost = labelCost;
		return this;
	}

	public CostConfigBuilder withAddLabelCost(Map<String, Integer> addLabelCost) {
		this.addLabelCost = addLabelCost;
		return this;
	}

	public CostConfigBuilder withRemoveLabelCost(Map<String, Integer> removeLabelCost) {
		this.removeLabelCost = removeLabelCost;
		return this;
	}

	public CostConfigBuilder withNodeFactor(Map<Node, Integer> nodeFactor) {
		this.nodeFactor = nodeFactor;
		return this;
	}

	public CostConfigBuilder withPinFactor(Map<Pin, Integer> pinFactor) {
		this.pinFactor = pinFactor;
		return this;
	}

	public CostConfigBuilder weighTFGs(boolean weigh) {
		this.weighTFGs = weigh;
		return this;
	}

	public CostConfig build() {
		return new CostConfig(addLabelCost, removeLabelCost, nodeFactor, pinFactor, weighTFGs);
	}

}
