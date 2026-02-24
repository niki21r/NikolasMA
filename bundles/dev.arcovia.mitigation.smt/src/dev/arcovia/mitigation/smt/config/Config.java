package dev.arcovia.mitigation.smt.config;

/**
 * Contains an input configuration for solving
 * @author Nikolas Rank
 *
 */

public class Config {

	private final boolean onlyRelevantModifications;
	private final boolean addNodeLabels;
	private final boolean removeNodeLabels;
	private final boolean addDataLabels;
	private final boolean removeDataLabels;
	private final CostConfig costConfig;
	private final boolean checkForViolationsAfter;
	private final boolean findExpressionTreeSize;
	private final boolean onlyViolatingTFGs;

	protected Config(boolean onlyRelevantLabels, boolean addNodeLabels, boolean removeNodeLabels, boolean addDataLabels,
			boolean removeDataLabels, CostConfig costConfig, boolean checkForViolationsAfter,
			boolean findExpressionTreeSize, boolean onlyViolatingTFGs) {
		this.onlyRelevantModifications = onlyRelevantLabels;
		this.addNodeLabels = addNodeLabels;
		this.removeNodeLabels = removeNodeLabels;
		this.addDataLabels = addDataLabels;
		this.removeDataLabels = removeDataLabels;
		this.costConfig = costConfig;
		this.findExpressionTreeSize = findExpressionTreeSize;
		this.checkForViolationsAfter = checkForViolationsAfter;
		this.onlyViolatingTFGs = onlyViolatingTFGs;
	}

	public boolean isFindExpressionTreeSize() {
		return findExpressionTreeSize;
	}

	public boolean isOnlyRelevantModifications() {
		return onlyRelevantModifications;
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

	public boolean isOnlyViolatingTFGs() {
		return onlyViolatingTFGs;
	}
	
}
