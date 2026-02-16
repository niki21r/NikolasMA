package dev.arcovia.mitigation.smt.config;

public class ConfigBuilder {

	private boolean onlyRelevantLabels = true;
	private boolean addNodeLabels = true;
	private boolean removeNodeLabels = true;
	private boolean addDataLabels = true;
	private boolean removeDataLabels = true;
	private CostConfig costConfig = new CostConfigBuilder().build();
	private boolean checkForViolationsAfter = false;
	private boolean findExpressionTreeSize = false;

	public ConfigBuilder() {
	}

	public ConfigBuilder onlyRelevantLabels(boolean onlyRelevantLabels) {
		this.onlyRelevantLabels = onlyRelevantLabels;
		return this;
	}

	public ConfigBuilder addNodeLabels(boolean addNodeLabels) {
		this.addNodeLabels = addNodeLabels;
		return this;
	}

	public ConfigBuilder removeNodeLabels(boolean removeNodeLabels) {
		this.removeNodeLabels = removeNodeLabels;
		return this;
	}

	public ConfigBuilder addDataLabels(boolean addDataLabels) {
		this.addDataLabels = addDataLabels;
		return this;
	}

	public ConfigBuilder removeDataLabels(boolean removeDataLabels) {
		this.removeDataLabels = removeDataLabels;
		return this;
	}

	public ConfigBuilder costConfig(CostConfig costConfig) {
		this.costConfig = costConfig;
		return this;
	}

	public ConfigBuilder checkForViolationsAfter(boolean checkForViolationsAfter) {
		this.checkForViolationsAfter = checkForViolationsAfter;
		return this;
	}

	public ConfigBuilder findExpressionTreeSize(boolean findExpressionTreeSize) {
		this.findExpressionTreeSize = findExpressionTreeSize;
		return this;
	}

	public Config build() {
		return new Config(onlyRelevantLabels, addNodeLabels, removeNodeLabels, addDataLabels, removeDataLabels,
				costConfig, checkForViolationsAfter, findExpressionTreeSize);
	}

}
