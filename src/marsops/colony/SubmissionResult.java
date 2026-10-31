package marsops.colony;

import marsops.missions.Mission;

/**
 * The outcome of submitting a mission to the colony: accepted, or rejected with a reason.
 * Immutable; created only through the factory methods.
 */
public final class SubmissionResult {

    private final Mission mission;
    private final boolean accepted;
    private final String reason;

    private SubmissionResult(Mission mission, boolean accepted, String reason) {
        this.mission = mission;
        this.accepted = accepted;
        this.reason = reason;
    }

    /**
     * Creates the result of an accepted mission.
     *
     * @param mission the accepted mission
     * @return an accepted result with an empty reason
     */
    public static SubmissionResult accepted(Mission mission) {
        return new SubmissionResult(mission, true, "");
    }

    /**
     * Creates the result of a rejected mission.
     *
     * @param mission the rejected mission
     * @param reason  why it was rejected
     * @return a rejected result carrying the reason
     */
    public static SubmissionResult rejected(Mission mission, String reason) {
        return new SubmissionResult(mission, false, reason == null ? "" : reason);
    }

    /** @return true if the mission was accepted */
    public boolean isAccepted() {
        return accepted;
    }

    /** @return the rejection reason, or "" when accepted */
    public String getReason() {
        return reason;
    }

    /** @return the mission this result is about */
    public Mission getMission() {
        return mission;
    }

    @Override
    public String toString() {
        if (accepted) {
            return "Mission " + mission.getId() + " accepted";
        }
        return "Mission cannot be assigned. Reason: " + reason;
    }
}