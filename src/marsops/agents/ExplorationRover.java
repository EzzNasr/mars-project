package marsops.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Equipment;
import marsops.common.Location;
import marsops.missions.Mission;

/**
 * A surface rover that carries equipment (aggregation).
 *
 * @author A
 */
public class ExplorationRover extends Robot {
    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.SURFACE_TRAVEL, Capability.VEHICLE));
    private static final int DRAIN = 5;

    private final List<Equipment> cargo = new ArrayList<>();

    /**
     * Creates a rover.
     *
     * @param id      unique id
     * @param name    display name
     * @param home    home location
     * @param battery initial battery, 0..100
     */
    public ExplorationRover(String id, String name, Location home, int battery) {
        super(id, name, home, battery);
    }

    /**
     * Creates a rover with a full battery.
     *
     * @param id   unique id
     * @param name display name
     * @param home home location
     */
    public ExplorationRover(String id, String name, Location home) {
        this(id, name, home, 100);
    }

    /**
     * Loads a piece of equipment onto the rover.
     *
     * @param e the equipment
     */
    public void loadEquipment(Equipment e) {
        if (e == null) throw new IllegalArgumentException("equipment required");
        cargo.add(e);
    }

    /** @return unmodifiable list of cargo */
    public List<Equipment> getCargo() { return Collections.unmodifiableList(cargo); }

    /**
     * Checks for a working piece of equipment.
     *
     * @param equipmentName the equipment name
     * @return true if present and functional
     */
    public boolean hasEquipment(String equipmentName) {
        for (Equipment e : cargo) {
            if (e.getName().equals(equipmentName) && e.isFunctional()) return true;
        }
        return false;
    }

    @Override public Set<Capability> getCapabilities() { return CAPABILITIES; }
    @Override protected int getDrainPerTick() { return DRAIN; }

    @Override
    public String getCannotPerformReason(Mission mission) {
        Set<Capability> required = mission.getRequiredCapabilities();
        if (!required.contains(Capability.SURFACE_TRAVEL) && !required.contains(Capability.VEHICLE)) {
            return lacksCapabilityReason();
        }
        String battery = batteryReason(mission);
        if (battery != null) return battery;
        for (String eq : mission.getRequiredEquipment()) {
            if (!hasEquipment(eq)) {
                return getName() + " is missing required equipment: " + eq;
            }
        }
        return null;
    }

    @Override public boolean canHandle(EmergencyType type) { return false; }
    @Override public String performFunction() { return "Exploring the surface."; }
}