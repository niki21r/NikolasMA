package dev.arcovia.mitigation.smt.config;

public record Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
		boolean removeDataLabels) {

	public Config() {
		this(true, true, true, true, true);
	}

	public Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
			boolean removeDataLabels) {
		this.onlyRelevantLabels = onlyRelevantLabels;
		this.addNodeLabels  = addNodeLabels;
		this.removeNodeLabels = removeNodeLabels;
		this.addDataLabels = addDataLabels;
		this.removeDataLabels = removeDataLabels;
	}
}
