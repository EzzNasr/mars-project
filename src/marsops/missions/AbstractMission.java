package marsops.missions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import marsops.agents.Agent;
import marsops.common.MissionState;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.facilities.Facility;

/**
 * Base class of all missions. It owns the lifecycle state machine and the progress
 * counter, so the four concrete missions only describe what they require.
 * {@link #onComplete()} is a hook (template method) that subclasses may override.
 *
 * @author B
 */
public abstract class AbstractMission implements Mission {

    private final String id;
    private final MissionType type;
    private final int durationUnits;
    private MissionState state = MissionState.CREATED;
    private int progressUnits = 0;
    private String failureReason;
    private final List<Agent> assignedAgents = new ArrayList<>();
    private final List<Facility> targetFacilities;
    private final ResourceRequirement resourceRequirement;

    /**
     * Creates a mission in state CREATED.
     *
     * @param id                  unique mission id, e.g. "M-1"
     * @param type                the mission type
     * @param durationUnits       how many time units the mission lasts (at least 1)
     * @param resourceRequirement resources consumed on acceptance (null means none)
     * @param targetFacilities    facilities the mission works on (null means none)
     * @throws IllegalArgumentException if id is blank, type is null or duration is below 1
     */
    protected AbstractMission(String id, MissionType type, int durationUnits,
                              ResourceRequirement resourceRequirement,
                              List<Facility> targetFacilities) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mission id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Mission type is required");
        }
        if (durationUnits < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 unit");
        }
        this.id = id;
        this.type = type;
        this.durationUnits = durationUnits;
        this.resourceRequirement = (resourceRequirement == null)
                ? new ResourceRequirement() : resourceRequirement;
        this.targetFacilities = (targetFacilities == null)
                ? new ArrayList<>() : new ArrayList<>(targetFacilities);
    }

    /**
     * Hook called exactly once, right after the mission becomes COMPLETED.
     * Subclasses override it to apply their effect (e.g. repair a facility).
     */
    protected void onComplete() { }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MissionType getType() {
        return type;
    }

    @Override
    public MissionState getState() {
        return state;
    }

    @Override
    public int getDurationUnits() {
        return durationUnits;
    }

    @Override
    public int getProgressUnits() {
        return progressUnits;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public List<Agent> getAssignedAgents() {
        return Collections.unmodifiableList(assignedAgents);
    }

    @Override
    public List<Facility> getTargetFacilities() {
        return Collections.unmodifiableList(targetFacilities);
    }

    @Override
    public ResourceRequirement getResourceRequirement() {
        return resourceRequirement;
    }
    /**
     * Moves the mission CREATED to ASSIGNED and remembers the team.
     *
     * @param agents the agents that will run the mission (not empty)
     * @throws IllegalStateException    if the mission is not CREATED
     * @throws IllegalArgumentException if agents is null or empty
     */
    @Override
    public void assign(List<Agent> agents) {
        if (state != MissionState.CREATED) {
            throw new IllegalStateException(
                    "Cannot assign mission " + id + " in state " + state);
        }
        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("At least one agent is required");
        }
        assignedAgents.clear();
        assignedAgents.addAll(agents);
        state = MissionState.ASSIGNED;
    }
    /**
     * Moves the mission ASSIGNED to IN_PROGRESS.
     *
     * @throws IllegalStateException if the mission is not ASSIGNED
     */
    @Override
    public void start() {
        if (state != MissionState.ASSIGNED) {
            throw new IllegalStateException(
                    "Cannot start mission " + id + " in state " + state);
        }
        state = MissionState.IN_PROGRESS;
    }
    /**
     * Adds one unit of progress. When progress reaches the duration the mission
     * becomes COMPLETED and {@link #onComplete()} is called once.
     *
     * @throws IllegalStateException if the mission is not IN_PROGRESS
     */
    @Override
    public void advance() {
        if (state != MissionState.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot advance mission " + id + " in state " + state);
        }
        progressUnits++;
        if (progressUnits >= durationUnits) {
            state = MissionState.COMPLETED;
            onComplete();
        }
    }
    /**
     * Marks the mission FAILED. COMPLETED and FAILED are final states.
     *
     * @param reason why the mission failed (not blank)
     * @throws IllegalStateException    if the mission is already COMPLETED or FAILED
     * @throws IllegalArgumentException if reason is null or blank
     */
    @Override
    public void fail(String reason) {
        if (isFinished()) {
            throw new IllegalStateException(
                    "Cannot fail mission " + id + " in state " + state);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A failure reason is required");
        }
        failureReason = reason;
        state = MissionState.FAILED;
    }

    @Override
    public boolean isFinished() {
        return state == MissionState.COMPLETED || state == MissionState.FAILED;
    }

    @Override
    public String describe() {
        return id + " [" + type + "] " + state + " " + progressUnits + "/" + durationUnits;
    }

    @Override
    public String toString() {
        return describe();
    }
}