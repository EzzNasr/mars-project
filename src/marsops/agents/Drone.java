package marsops.agents;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.missions.Mission;

/**
 * A remote-controlled aerial drone.
 *
 * @author A
 */
public class Drone extends Robot {
    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.AERIAL_RECON, Capability.VEHICLE));
    private static final int DRAIN = 8;

    /**
     * Creates a drone.
     *
     * @param id      unique id
     * @param name    display name
     * @param home    home location
     * @param battery initial battery, 0..100
     */
    public Drone(String id, String name, Location home, int battery) {
        super(id, name, home, battery);
    }

    /**
     * Creates a drone with a full battery.
     *
     * @param id   unique id
     * @param name display name
     * @param home home location
     */
    public Drone(String id, String name, Location home) {
        this(id, name, home, 100);
    }

    @Override public Set<Capability> getCapabilities() { return CAPABILITIES; }
    @Override protected int getDrainPerTick() { return DRAIN; }

    @Override
    public String getCannotPerformReason(Mission mission) {
        Set<Capability> required = mission.getRequiredCapabilities();
        if (!required.contains(Capability.VEHICLE) && !required.contains(Capability.AERIAL_RECON)) {
            return lacksCapabilityReason();
        }
        return batteryReason(mission);
    }

    @Override public boolean canHandle(EmergencyType type) { return false; }
    @Override public boolean isRemoteControlled() { return true; }
    @Override public String performFunction() { return "Performing aerial reconnaissance."; }
}