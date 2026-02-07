package dev.arcovia.mitigation.smt.config;

import java.util.HashMap;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class CostConfigBuilder {

	public HashMap<String, Integer> addLabelCost;
	public HashMap<String, Integer> removeLabelCost;
	public HashMap<Node, Integer> nodeFactor;
	public HashMap<Pin, Integer> pinFactor;
	public boolean weighTFGs;
	
	public CostConfigBuilder() {
		addLabelCost = new HashMap<>();
		removeLabelCost = new HashMap<>();
		nodeFactor = new HashMap<>();
		pinFactor = new HashMap<>();
		weighTFGs = false;
	}
	
	public CostConfigBuilder withLabelCost(HashMap<String, Integer> labelCost) {
		addLabelCost = labelCost;
		removeLabelCost = labelCost;
		return this;
	}
	
	public CostConfigBuilder withAddLabelCost(HashMap<String, Integer> addLabelCost) {
		this.addLabelCost = addLabelCost;
		return this;
	}
	
	public CostConfigBuilder withRemoveLabelCost(HashMap<String, Integer> removeLabelCost) {
		this.removeLabelCost = removeLabelCost;
		return this;
	}
	
	public CostConfigBuilder withNodeFactor(HashMap<Node, Integer> nodeFactor) {
		this.nodeFactor = nodeFactor;
		return this;
	}
	
	public CostConfigBuilder withPinFactor(HashMap<Pin, Integer> pinFactor) {
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
