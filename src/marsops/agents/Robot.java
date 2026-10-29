package marsops.agents;

import marsops.common.Location;
import marsops.common.RobotCondition;
import marsops.missions.Mission;

/**
 * A robotic agent with a battery and a damage condition.
 *
 * @author A
 */
public abstract class Robot implements Agent {
    public static final int DAMAGE_THRESHOLD = 30;
    public static final int IDLE_RECHARGE = 1;

    private final String id;
    private final String name;
    private final Location home;
    private Location location;
    private int battery;
    private RobotCondition condition = RobotCondition.OPERATIONAL;
    private boolean assigned = false;

    /**
     * Creates an operational robot at its home location.
     *
     * @param id      unique id
     * @param name    display name
     * @param home    starting and return location
     * @param battery initial battery, 0..100
     * @throws IllegalArgumentException if an argument is invalid or battery is outside 0..100
     */
    protected Robot(String id, String name, Location home, int battery) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (home == null) throw new IllegalArgumentException("home required");
        if (battery < 0 || battery > 100) throw new IllegalArgumentException("battery must be 0..100");
        this.id = id;
        this.name = name;
        this.home = home;
        this.location = home;
        this.battery = battery;
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public Location getLocation() { return location; }

    @Override
    public void setLocation(Location location) {
        if (location == null) throw new IllegalArgumentException("location required");
        this.location = location;
    }

    @Override public Location getHomeLocation() { return home; }

    /** @return battery level, 0..100 */
    public int getBattery() { return battery; }

    /**
     * Drains the battery, never below 0.
     *
     * @param amount units to drain
     */
    public void drain(int amount) { battery = Math.max(0, battery - amount); }

    /**
     * Recharges the battery, never above 100.
     *
     * @param amount units to add
     */
    public void recharge(int amount) { battery = Math.min(100, battery + amount); }

    /** @return the robot's condition */
    public RobotCondition getCondition() { return condition; }

    /** Marks the robot as damaged. */
    public void damage() { condition = RobotCondition.DAMAGED; }

    /** Restores the robot to operational. */
    public void repair() { condition = RobotCondition.OPERATIONAL; }

    /** @return battery units lost per tick while assigned */
    protected abstract int getDrainPerTick();

    /** @return a short description of what this robot does (polymorphism demo) */
    public abstract String performFunction();

    /** @return the standard "lacks capability" sentence */
    protected String lacksCapabilityReason() {
        return name + " does not possess the required capability";
    }

    /**
     * Checks the battery against the mission minimum.
     *
     * @param m the mission
     * @return null if the battery is enough, otherwise a reason
     */
    protected String batteryReason(Mission m) {
        int min = m.getMinBatteryPercent();
        if (battery >= min) return null;
        return name + " battery (" + battery + "%) is below the required " + min + "%";
    }

    @Override
    public String getUnusableReason() {
        if (condition == RobotCondition.DAMAGED) return name + " is damaged";
        if (battery == 0) return name + " battery is depleted";
        return null;
    }

    @Override public boolean isAssigned() { return assigned; }

    @Override
    public void assignToMission() {
        if (assigned) throw new IllegalStateException(name + " is already assigned");
        assigned = true;
    }

    @Override public void release() { assigned = false; }

    @Override public boolean isRemoteControlled() { return false; }

    @Override
    public void takeDamage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        if (amount >= DAMAGE_THRESHOLD) damage();
    }

    @Override
    public void recover(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        if (amount >= DAMAGE_THRESHOLD) repair();
    }

    @Override
    public void tick() {
        if (assigned) {
            drain(getDrainPerTick());
        } else {
            recharge(IDLE_RECHARGE);
        }
    }

    @Override
    public String describe() {
        String state = isUsable() ? "usable " + (assigned ? "assigned" : "available") : "unusable";
        return name + " [" + getClass().getSimpleName() + "] @" + location.getName()
                + " battery=" + battery + " " + condition + " " + state;
    }
}
