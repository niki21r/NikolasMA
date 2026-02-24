package dev.arcovia.mitigation.smt.config;

import java.util.HashMap;

import java.util.Map;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

/**
 * A convenient builder for the creation of cost function configurations
 * @author Nikolas Rank
 *
 */
public class CostConfigBuilder {

	// Label Addition cost
	private Map<String, Integer> addLabelCost = new HashMap<>();
	// Label removal cost
	private Map<String, Integer> removeLabelCost = new HashMap<>();
	// User defined factors for nodes and pins. 
	private Map<Node, Integer> nodeFactor = new HashMap<>();
	private Map<Pin, Integer> pinFactor = new HashMap<>();
	// This flag indicates whether the user wants to weigh nodes and pins according to their prevalence in TFGs.
	// If true, earlier configured node and pin factors will be overwritten during cost function creation.
	private boolean weighTFGs = false;

	public CostConfigBuilder() {
	}

	/**
	 * Sets the defined cost for label modification. This method offers an entrypoint for 
	 * configurations that have the same cost for addition and removal of a specific label
	 * @param labelCost Map of the label costs
	 * @return The builder
	 */
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
