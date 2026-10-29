package marsops.emergencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import marsops.agents.Agent; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyState;

// Handles the emergencies of the colony.
// It picks responders by asking the agents (canHandle), checks resources and solves or fails.
// A failed emergency is never retried.
public class EmergencyManager {
    private final MarsColony colony;
    private final List<Emergency> history = new ArrayList<>();

    public EmergencyManager(MarsColony colony) {
        if (colony == null) {
            throw new IllegalArgumentException("colony required");
        }
        this.colony = colony;
    }

    // main method, follows the steps from the playbook (section 10.2)
    public Emergency handle(Emergency emergency) {
        if (emergency == null) {
            throw new IllegalArgumentException("emergency required");
        }
        // step 1: save it, log it and apply the first effects
        history.add(emergency);
        colony.log("Emergency " + emergency.getId() + " [" + emergency.getType() + "] detected at " // waiting for B's commit
                + emergency.getLocation().getName());
        emergency.applyInitialEffects(colony);

        // steps 2 to 5: each step gives a failure text, null means it worked
        List<Agent> chosen = new ArrayList<>();
        String failure = emergency.checkPreconditions(colony);
        if (failure == null) {
            failure = pickResponders(emergency, chosen);
        }
        if (failure == null) {
            failure = colony.getResourceManager().explainShortage(emergency.getResponseResources()); // waiting for B's commit
        }
        if (failure == null) {
            failure = respond(emergency, chosen);
        }

        // step 7: something failed, so apply the penalty
        if (failure != null) {
            emergency.applyUnresolvedPenalty(colony);
            emergency.setState(EmergencyState.UNRESOLVED, failure);
        }
        colony.log("Emergency outcome: " + emergency.describe()); // waiting for B's commit
        return emergency;
    }

    // all emergencies in the order they were handled
    public List<Emergency> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // only the emergencies that are UNRESOLVED
    public List<Emergency> getUnresolved() {
        List<Emergency> result = new ArrayList<>();
        for (Emergency e : history) {
            if (e.getState() == EmergencyState.UNRESOLVED) {
                result.add(e);
            }
        }
        return result;
    }

    // choose one different agent for each needed capability
    // returns null if ok, otherwise the reason
    private String pickResponders(Emergency emergency, List<Agent> chosen) {
        for (Capability capability : emergency.getRequiredResponderCapabilities()) {
            Agent found = null;
            for (Agent agent : colony.getAgents()) { // waiting for B's commit
                if (!chosen.contains(agent)
                        && !emergency.isExcluded(agent)
                        && agent.isAvailable() // waiting for A's commit
                        && agent.canHandle(emergency.getType()) // waiting for A's commit
                        && agent.hasCapability(capability)) { // waiting for A's commit
                    found = agent;
                    break;
                }
            }
            if (found == null) {
                return "No available responder with capability " + capability;
            }
            chosen.add(found);
        }
        return null;
    }

    // send the responders, use the resources and solve the emergency
    // returns null if ok, otherwise the reason
    private String respond(Emergency emergency, List<Agent> chosen) {
        List<String> names = new ArrayList<>();
        for (Agent agent : chosen) {
            agent.assignToMission(); // waiting for A's commit
            agent.setLocation(emergency.getLocation()); // waiting for A's commit
            emergency.addResponder(agent);
            names.add(agent.getName());
        }
        if (!colony.getResourceManager().consume(emergency.getResponseResources())) { // waiting for B's commit
            releaseAll(chosen);
            return "Response resources could not be consumed";
        }
        emergency.applyResolution(colony);
        emergency.setState(EmergencyState.RESOLVED, "Repaired by " + String.join(", ", names));
        // responders stay at the emergency location (D-12)
        releaseAll(chosen);
        return null;
    }

    // make the agents free again
    private void releaseAll(List<Agent> agents) {
        for (Agent agent : agents) {
            agent.release(); // waiting for A's commit
        }
    }
}
