package marsops.emergencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import marsops.agents.Agent;
import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyState;

// Handles the emergencies. It asks the agents who can help (canHandle),
// checks the resources and then fixes the emergency or gives up.
// An emergency that failed is never tried again.
public class EmergencyManager {
    private final MarsColony colony;
    private final List<Emergency> history = new ArrayList<>();

    public EmergencyManager(MarsColony colony) {
        if (colony == null) {
            throw new IllegalArgumentException("colony required");
        }
        this.colony = colony;
    }

    public Emergency handle(Emergency e) {
        history.add(e);
        colony.log("Emergency " + e.getId() + " detected at " + e.getLocation().getName());
        e.applyInitialEffects(colony);

        // each check gives back the problem as text, null means no problem
        List<Agent> team = new ArrayList<>();
        String problem = e.checkPreconditions(colony);
        if (problem == null) {
            problem = pickResponders(e, team);
        }
        if (problem == null) {
            problem = colony.getResourceManager().explainShortage(e.getResponseResources());
        }

        if (problem != null) {
            e.applyUnresolvedPenalty(colony);
            e.setState(EmergencyState.UNRESOLVED, problem);
        } else {
            solve(e, team);
        }
        colony.log("Emergency result: " + e.describe());
        return e;
    }

    public List<Emergency> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Emergency> getUnresolved() {
        List<Emergency> result = new ArrayList<>();
        for (Emergency e : history) {
            if (e.getState() == EmergencyState.UNRESOLVED) {
                result.add(e);
            }
        }
        return result;
    }

    // for every capability we take the first free agent that can help
    // (and that is not already in the team). returns null if we found everybody
    private String pickResponders(Emergency e, List<Agent> team) {
        for (Capability capability : e.getRequiredResponderCapabilities()) {
            Agent found = null;
            for (Agent a : colony.getAgents()) {
                if (!team.contains(a) && !e.isExcluded(a) && a.isAvailable()
                        && a.canHandle(e.getType()) && a.hasCapability(capability)) {
                    found = a;
                    break;
                }
            }
            if (found == null) {
                return "No available responder with capability " + capability;
            }
            team.add(found);
        }
        return null;
    }

    // send the team, pay for the response and fix the emergency
    private void solve(Emergency e, List<Agent> team) {
        List<String> names = new ArrayList<>();
        for (Agent a : team) {
            a.assignToMission();
            a.setLocation(e.getLocation());
            e.addResponder(a);
            names.add(a.getName());
        }
        colony.getResourceManager().consume(e.getResponseResources());
        e.applyResolution(colony);
        e.setState(EmergencyState.RESOLVED, "Repaired by " + String.join(", ", names));

        // free again, but they stay where the emergency was
        for (Agent a : team) {
            a.release();
        }
    }
}
