package marsops.emergencies;

import java.util.List;

import marsops.agents.ExplorationRover;
import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.missions.Mission; // B's class

// A rover breaks down where it is standing.
public class RoverBreakdown extends Emergency {
    private final ExplorationRover rover;

    public RoverBreakdown(String id, ExplorationRover rover) {
        super(id, EmergencyType.ROVER_BREAKDOWN, rover.getLocation());
        this.rover = rover;
    }

    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR);
    }

    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.SPARE_PARTS, 2)
                .with(ResourceType.BATTERY_POWER, 5);
    }

    // break the rover first, then stop the mission if the rover was in it
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        rover.damage();
        Mission mission = colony.getActiveMission();
        if (mission != null && mission.getAssignedAgents().contains(rover)) {
            colony.failActiveMission("Rover " + rover.getName() + " broke down");
        }
    }

    @Override
    protected void applyResolution(MarsColony colony) {
        rover.repair();
        rover.setLocation(rover.getHomeLocation());
    }

    // nothing else happens, the rover just stays broken
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
    }
}
