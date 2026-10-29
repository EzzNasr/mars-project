package marsops.facilities;

import marsops.common.Equipment;

/**
 * A laboratory: research happens here; it can catch fire and lose its equipment.
 *
 * @author A
 */
public class Laboratory extends Facility {
    private boolean onFire = false;

    /**
     * Creates a laboratory.
     *
     * @param name     facility name
     * @param capacity maximum total units storable
     */
    public Laboratory(String name, int capacity) {
        super(name, capacity);
    }

    /** @return true while the lab is on fire */
    public boolean isOnFire() { return onFire; }

    /**
     * Sets the fire flag.
     *
     * @param onFire true if on fire
     */
    public void setOnFire(boolean onFire) { this.onFire = onFire; }

    /** Damages every piece of equipment in the lab. */
    public void damageAllEquipment() {
        for (Equipment e : getEquipment()) {
            e.damage();
        }
    }

    /**
     * Checks for working equipment.
     *
     * @param equipmentName the equipment name
     * @return true if present and functional
     */
    public boolean hasFunctionalEquipment(String equipmentName) {
        Equipment e = findEquipment(equipmentName);
        return e != null && e.isFunctional();
    }

    @Override public String getKind() { return "Laboratory"; }
}
