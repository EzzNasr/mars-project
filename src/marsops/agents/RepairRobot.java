package marsops.agents;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.missions.Mission;

/**
 * A robot that repairs equipment and responds to technical emergencies.
 *
 * @author A
 */
public class RepairRobot extends Robot {
    private static final Set<Capability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(Capability.ROBOTIC_REPAIR));
    private static final int DRAIN = 3;

    /**
     * Creates a repair robot.
     *
     * @param id      unique id
     * @param name    display name
     * @param home    home location
     * @param battery initial battery, 0..100
     */
    public RepairRobot(String id, String name, Location home, int battery) {
        super(id, name, home, battery);
    }

    /**
     * Creates a repair robot with a full battery.
     *
     * @param id   unique id
     * @param name display name
     * @param home home location
     */
    public RepairRobot(String id, String name, Location home) {
        this(id, name, home, 100);
    }

    @Override public Set<Capability> getCapabilities() { return CAPABILITIES; }
    @Override protected int getDrainPerTick() { return DRAIN; }

    @Override
    public String getCannotPerformReason(Mission mission) {
        if (!mission.getRequiredCapabilities().contains(Capability.ROBOTIC_REPAIR)) {
            return lacksCapabilityReason();
        }
        return batteryReason(mission);
    }

    @Override
    public boolean canHandle(EmergencyType type) {
        return type == EmergencyType.OXYGEN_LEAK || type == EmergencyType.ROVER_BREAKDOWN;
    }

    @Override public String performFunction() { return "Repairing equipment inside facilities."; }
}
