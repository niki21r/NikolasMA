package dev.arcovia.mitigation.smt.config;

public class Config {

	private final boolean onlyRelevantLabels;
	private final boolean addNodeLabels;
	private final boolean removeNodeLabels;
	private final boolean addDataLabels;
	private final boolean removeDataLabels;
	private final CostConfig costConfig;
	private final boolean checkForViolationsAfter;
	private final boolean findExpressionTreeSize;

	protected Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
			boolean removeDataLabels, CostConfig costConfig, boolean checkForViolationsAfter,
			boolean findExpressionTreeSize) {
		this.onlyRelevantLabels = onlyRelevantLabels;
		this.addNodeLabels = addNodeLabels;
		this.removeNodeLabels = removeNodeLabels;
		this.addDataLabels = addDataLabels;
		this.removeDataLabels = removeDataLabels;
		this.costConfig = costConfig;
		this.findExpressionTreeSize = findExpressionTreeSize;
		this.checkForViolationsAfter = checkForViolationsAfter;
	}

	public boolean isFindExpressionTreeSize() {
		return findExpressionTreeSize;
	}

	public boolean isOnlyRelevantLabels() {
		return onlyRelevantLabels;
	}

	public boolean isAddNodeLabels() {
		return addNodeLabels;
	}

	public boolean isRemoveNodeLabels() {
		return removeNodeLabels;
	}

	public boolean isAddDataLabels() {
		return addDataLabels;
	}

	public boolean isRemoveDataLabels() {
		return removeDataLabels;
	}

	public CostConfig getCostConfig() {
		return costConfig;
	}

	public boolean isCheckForViolationsAfter() {
		return checkForViolationsAfter;
	}

}
