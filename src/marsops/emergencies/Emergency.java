package marsops.emergencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import marsops.agents.Agent;
import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyState;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.common.ResourceRequirement;

// Parent of all the emergencies. Every emergency knows what it does to the colony
// when it starts, when we fix it and when we fail to fix it.
public abstract class Emergency {
    private final String id;
    private final EmergencyType type;
    private final Location location;
    private EmergencyState state = EmergencyState.DETECTED;
    private String reason = "";
    private final List<Agent> responders = new ArrayList<>();

    protected Emergency(String id, EmergencyType type, Location location) {
        if (id == null || id.isBlank() || type == null || location == null) {
            throw new IllegalArgumentException("id, type and location are required");
        }
        this.id = id;
        this.type = type;
        this.location = location;
    }

    public String getId() { return id; }
    public EmergencyType getType() { return type; }
    public Location getLocation() { return location; }
    public EmergencyState getState() { return state; }

    // the reason it failed, or a small note when it was fixed
    public String getReason() { return reason; }

    public List<Agent> getResponders() { return Collections.unmodifiableList(responders); }

    // example: E-1 [OXYGEN_LEAK] RESOLVED - Repaired by Eng. Omar, Fixer-1
    public String describe() {
        return id + " [" + type + "] " + state + " - " + reason;
    }

    // who we need to send (one different agent for each capability)
    public abstract List<Capability> getRequiredResponderCapabilities();

    // what the response costs
    public abstract ResourceRequirement getResponseResources();

    // happens as soon as the emergency starts
    protected abstract void applyInitialEffects(MarsColony colony);

    // happens if somebody fixed it
    protected abstract void applyResolution(MarsColony colony);

    // happens if nobody could fix it
    protected abstract void applyUnresolvedPenalty(MarsColony colony);

    // return null when it's ok, or the reason we can't handle it
    protected String checkPreconditions(MarsColony colony) {
        return null;
    }

    // some agents must never answer the call (like the patient)
    protected boolean isExcluded(Agent agent) {
        return false;
    }

    // the two methods below are only for EmergencyManager
    void setState(EmergencyState state, String reason) {
        this.state = state;
        this.reason = reason;
    }

    void addResponder(Agent agent) {
        responders.add(agent);
    }
}
