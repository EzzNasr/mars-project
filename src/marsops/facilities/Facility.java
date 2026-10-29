package marsops.facilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import marsops.common.Equipment;
import marsops.common.FacilityStatus;
import marsops.common.Location;
import marsops.common.ResourceType;

/**
 * A colony facility: stores resources (composition) and holds equipment (aggregation).
 *
 * @author A
 */
public abstract class Facility implements Location {
    private final String name;
    private final int capacity;
    private FacilityStatus status = FacilityStatus.OPERATIONAL;
    private final ResourceInventory inventory = new ResourceInventory();
    private final List<Equipment> equipment = new ArrayList<>();

    /**
     * Creates an operational, empty facility.
     *
     * @param name     facility name
     * @param capacity maximum total units storable
     * @throws IllegalArgumentException if name is blank or capacity is not positive
     */
    protected Facility(String name, int capacity) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.name = name;
        this.capacity = capacity;
    }

    @Override public String getName() { return name; }

    /** @return maximum total resource units this facility can store (D-18) */
    public int getCapacity() { return capacity; }

    /** @return current status */
    public FacilityStatus getStatus() { return status; }

    /**
     * Changes the status.
     *
     * @param status the new status
     * @throws IllegalArgumentException if null
     */
    public void setStatus(FacilityStatus status) {
        if (status == null) throw new IllegalArgumentException("status required");
        this.status = status;
    }

    /** @return true if status is OPERATIONAL */
    public boolean isOperational() { return status == FacilityStatus.OPERATIONAL; }

    /**
     * Raw stock, ignoring status.
     *
     * @param type the resource
     * @return units stored
     */
    public int getStored(ResourceType type) { return inventory.get(type); }

    /**
     * Stock the colony may use: 0 unless the facility is operational (D-02).
     *
     * @param type the resource
     * @return usable units
     */
    public int getAvailable(ResourceType type) {
        return isOperational() ? getStored(type) : 0;
    }

    /** @return total units stored of all resources */
    public int getTotalStored() { return inventory.total(); }

    /**
     * Stores resources if capacity allows.
     *
     * @param type   the resource
     * @param amount units to store (must be positive)
     * @return true if stored; false (no change) if capacity would be exceeded
     */
    public boolean store(ResourceType type, int amount) {
        if (type == null || amount <= 0) {
            throw new IllegalArgumentException("type required and amount must be > 0");
        }
        if (getTotalStored() + amount > capacity) return false;
        inventory.add(type, amount);
        return true;
    }

    /**
     * Withdraws resources only if enough are stored.
     *
     * @param type   the resource
     * @param amount units to withdraw (must be positive)
     * @return true if withdrawn; false (no change) if stock is too low
     */
    public boolean withdraw(ResourceType type, int amount) {
        return inventory.remove(type, amount);
    }

    /**
     * Loses resources (leak, fire) without ever going below zero.
     *
     * @param type   the resource
     * @param amount units to lose (not negative)
     * @return units actually lost
     */
    public int lose(ResourceType type, int amount) {
        return inventory.removeUpTo(type, amount);
    }

    /**
     * Adds equipment to this facility.
     *
     * @param e the equipment
     * @throws IllegalArgumentException if null
     */
    public void addEquipment(Equipment e) {
        if (e == null) throw new IllegalArgumentException("equipment required");
        equipment.add(e);
    }

    /** @return unmodifiable list of equipment */
    public List<Equipment> getEquipment() { return Collections.unmodifiableList(equipment); }

    /**
     * Finds equipment by name.
     *
     * @param equipmentName the name
     * @return the equipment, or null if none
     */
    public Equipment findEquipment(String equipmentName) {
        for (Equipment e : equipment) {
            if (e.getName().equals(equipmentName)) return e;
        }
        return null;
    }

    /** @return the kind of facility, e.g. "Habitat" */
    public abstract String getKind();

    /** @return one-line status, e.g. "Habitat A [Habitat] OPERATIONAL OXYGEN=300 WATER=150" */
    public String describe() {
        StringBuilder sb = new StringBuilder(name)
                .append(" [").append(getKind()).append("] ").append(status);
        for (Map.Entry<ResourceType, Integer> entry : inventory.snapshot().entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        return sb.toString();
    }

    @Override public String toString() { return name; }
}
