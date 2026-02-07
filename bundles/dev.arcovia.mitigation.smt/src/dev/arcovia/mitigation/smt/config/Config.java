package dev.arcovia.mitigation.smt.config;

public record Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
		boolean removeDataLabels, CostConfig costConfig) {

	public Config() {
		this(true, true, true, true, true, new CostConfigBuilder().build());
	}

	public Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
			boolean removeDataLabels, CostConfig costConfig) {
		this.onlyRelevantLabels = onlyRelevantLabels;
		this.addNodeLabels  = addNodeLabels;
		this.removeNodeLabels = removeNodeLabels;
		this.addDataLabels = addDataLabels;
		this.removeDataLabels = removeDataLabels;
		this.costConfig = costConfig;
	}
}
