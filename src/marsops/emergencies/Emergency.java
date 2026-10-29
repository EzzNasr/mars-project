package marsops.emergencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import marsops.agents.Agent; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyState;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.common.ResourceRequirement;

// Base class for all emergencies (oxygen leak, fire, ...).
// Each emergency knows what happens when it starts, when it is solved and when it is not solved.
public abstract class Emergency {
    private final String id;
    private final EmergencyType type;
    private final Location location;
    private EmergencyState state = EmergencyState.DETECTED;
    private String reason = "";
    private final List<Agent> responders = new ArrayList<>();

    // check the arguments first, then save them
    protected Emergency(String id, EmergencyType type, Location location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("emergency id required");
        }
        if (type == null) {
            throw new IllegalArgumentException("emergency type required");
        }
        if (location == null) {
            throw new IllegalArgumentException("emergency location required");
        }
        this.id = id;
        this.type = type;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public EmergencyType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public EmergencyState getState() {
        return state;
    }

    // why it was not solved, or a short success message
    public String getReason() {
        return reason;
    }

    // unmodifiable so nobody can change the list from outside
    public List<Agent> getResponders() {
        return Collections.unmodifiableList(responders);
    }

    // one line text, for example: E-1 [OXYGEN_LEAK] RESOLVED - Repaired by Omar
    public String describe() {
        return id + " [" + type + "] " + state + " - " + reason;
    }

    // which capabilities the responders need (D-10)
    public abstract List<Capability> getRequiredResponderCapabilities();

    // which resources the response uses (D-11)
    public abstract ResourceRequirement getResponseResources();

    // what happens when the emergency is detected
    protected abstract void applyInitialEffects(MarsColony colony);

    // what happens when the emergency is solved
    protected abstract void applyResolution(MarsColony colony);

    // what happens when the emergency is not solved
    protected abstract void applyUnresolvedPenalty(MarsColony colony);

    // returns null if everything is ok, otherwise the reason it cannot be handled
    protected String checkPreconditions(MarsColony colony) {
        return null;
    }

    // agents that can never respond (for example the patient)
    protected boolean isExcluded(Agent agent) {
        return false;
    }

    // only used by EmergencyManager
    void setState(EmergencyState state, String reason) {
        if (state == null) {
            throw new IllegalArgumentException("state required");
        }
        this.state = state;
        this.reason = reason == null ? "" : reason;
    }

    // only used by EmergencyManager
    void addResponder(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent required");
        }
        responders.add(agent);
    }
}
