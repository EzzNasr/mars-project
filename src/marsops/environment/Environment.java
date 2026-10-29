package marsops.environment;

import marsops.common.EnvironmentCondition;

/**
 * The current environmental condition of the colony.
 *
 * @author A
 */
public class Environment {
    private EnvironmentCondition condition = EnvironmentCondition.NORMAL;

    /** @return the current condition */
    public EnvironmentCondition getCondition() { return condition; }

    /**
     * Changes the condition.
     *
     * @param condition the new condition
     * @throws IllegalArgumentException if null
     */
    public void setCondition(EnvironmentCondition condition) {
        if (condition == null) throw new IllegalArgumentException("condition required");
        this.condition = condition;
    }

    /** @return true if NORMAL */
    public boolean isNormal() { return condition == EnvironmentCondition.NORMAL; }

    /** @return true if DUST_STORM */
    public boolean isDustStorm() { return condition == EnvironmentCondition.DUST_STORM; }

    /** @return true if COMMUNICATION_DISRUPTION */
    public boolean isCommunicationDisrupted() {
        return condition == EnvironmentCondition.COMMUNICATION_DISRUPTION;
    }

    /** @return e.g. "Environment: DUST_STORM" */
    public String describe() { return "Environment: " + condition; }
}
