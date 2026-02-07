package dev.arcovia.mitigation.smt.config;

import java.util.HashMap;

import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class CostConfig {

	public HashMap<String, Integer> addLabelCost;
	public HashMap<String, Integer> removeLabelCost;
	public HashMap<Node, Integer> nodeFactor;
	public HashMap<Pin, Integer> pinFactor;
	public boolean weighTFGs;
	
	protected CostConfig(HashMap<String, Integer> addLabelCost, HashMap<String, Integer> removeLabelCost,
			HashMap<Node, Integer> nodeFactor, HashMap<Pin, Integer> pinFactor, boolean weighTFGs) {
		this.addLabelCost = addLabelCost;
		this.removeLabelCost = removeLabelCost;
		this.nodeFactor = nodeFactor;
		this.pinFactor = pinFactor;
		this.weighTFGs = weighTFGs;
	}	
}
