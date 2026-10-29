package marsops.emergencies;

import java.util.List;

import marsops.agents.ExplorationRover; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.missions.Mission; // waiting for B's commit

// Rover breakdown (D-15).
// The rover is damaged where it stands. If it is in the active mission, the mission fails.
public class RoverBreakdown extends Emergency {
    private final ExplorationRover rover;

    // the location is where the rover is when the emergency is created
    public RoverBreakdown(String id, ExplorationRover rover) {
        super(id, EmergencyType.ROVER_BREAKDOWN, requireRover(rover).getLocation());
        this.rover = rover;
    }

    // used before super() so the rover is checked first
    private static ExplorationRover requireRover(ExplorationRover rover) {
        if (rover == null) {
            throw new IllegalArgumentException("rover required");
        }
        return rover;
    }

    // needs an engineer and a repair robot
    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR);
    }

    // 2 spare parts and 5 battery power
    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.SPARE_PARTS, 2)
                .with(ResourceType.BATTERY_POWER, 5);
    }

    // damage the rover first, then fail the active mission if the rover is part of it
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        rover.damage(); // waiting for A's commit
        Mission active = colony.getActiveMission(); // waiting for B's commit
        if (active != null && active.getAssignedAgents().contains(rover)) { // waiting for B's commit
            colony.failActiveMission("Rover " + rover.getName() + " broke down"); // waiting for B's commit
        }
    }

    // rover is repaired and goes back home
    @Override
    protected void applyResolution(MarsColony colony) {
        rover.repair(); // waiting for A's commit
        rover.setLocation(rover.getHomeLocation()); // waiting for A's commit
    }

    // nothing happens, the rover stays damaged where it is
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
    }
}
