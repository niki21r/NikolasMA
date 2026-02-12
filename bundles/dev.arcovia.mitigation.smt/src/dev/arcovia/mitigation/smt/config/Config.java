package dev.arcovia.mitigation.smt.config;

public record Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
		boolean removeDataLabels, CostConfig costConfig, boolean checkForViolationsAfter, boolean findExpressionTreeSize) {

	public Config() {
		this(true, true, true, true, true, new CostConfigBuilder().build(), false, false);
	}

	public Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
			boolean removeDataLabels, CostConfig costConfig, boolean checkForViolationsAfter, boolean findExpressionTreeSize) {
		this.onlyRelevantLabels = onlyRelevantLabels;
		this.addNodeLabels  = addNodeLabels;
		this.removeNodeLabels = removeNodeLabels;
		this.addDataLabels = addDataLabels;
		this.removeDataLabels = removeDataLabels;
		this.costConfig = costConfig;
		this.findExpressionTreeSize = findExpressionTreeSize;
		this.checkForViolationsAfter = checkForViolationsAfter;
	}
}
