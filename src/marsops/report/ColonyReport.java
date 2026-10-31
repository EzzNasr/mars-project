package marsops.report;

import java.util.List;
import java.util.Map;
import marsops.agents.Agent;
import marsops.colony.MarsColony;
import marsops.common.MissionState;
import marsops.common.ResourceType;
import marsops.emergencies.Emergency;
import marsops.facilities.Facility;
import marsops.missions.Mission;

/**
 * Generates a comprehensive status report for the Mars colony.
 *
 * @author A
 */
public final class ColonyReport {

    private ColonyReport() {
        // Prevent instantiation
    }

    /**
     * Builds a multi-line text report of the colony's current status.
     *
     * @param colony      the Mars colony to report on
     * @param currentTime the current simulation time
     * @param emergencies the history of emergencies to include in the report
     * @return a formatted multi-line string containing the colony report
     */
    public static String build(MarsColony colony, int currentTime, List<Emergency> emergencies) {
        StringBuilder sb = new StringBuilder();

        // 1. Header
        sb.append("=== MARS-OPS COLONY STATUS REPORT (t=").append(currentTime).append(") ===\n");

        // 2. Environment line
        sb.append(colony.getEnvironment().describe()).append("\n");

        // 3. Colony-wide resource totals (one line per ResourceType)
        Map<ResourceType, Integer> totals = colony.getResourceManager().getAllTotals();
        for (ResourceType type : ResourceType.values()) {
            sb.append(type.name()).append(": ").append(totals.getOrDefault(type, 0)).append("\n");
        }

        // 4. Facilities
        for (Facility facility : colony.getFacilities()) {
            sb.append(facility.describe()).append("\n");
        }

        // 5. Agents
        for (Agent agent : colony.getAgents()) {
            sb.append(agent.describe()).append("\n");
        }

        // 6. Active mission
        Mission active = colony.getActiveMission();
        if (active == null) {
            sb.append("none\n");
        } else {
            sb.append(active.describe()).append("\n");
        }

        // 7. Mission counts
        int completed = 0;
        int failed = 0;
        for (Mission m : colony.getMissionHistory()) {
            if (m.getState() == MissionState.COMPLETED) {
                completed++;
            } else if (m.getState() == MissionState.FAILED) {
                failed++;
            }
        }
        sb.append("completed=").append(completed).append(" failed=").append(failed).append("\n");

        // 8. Emergencies
        if (emergencies == null || emergencies.isEmpty()) {
            sb.append("none\n");
        } else {
            for (Emergency e : emergencies) {
                sb.append(e.describe()).append("\n");
            }
        }

        return sb.toString();
    }
}