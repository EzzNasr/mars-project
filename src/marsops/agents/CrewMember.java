package marsops.agents;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import marsops.common.Location;
import marsops.common.Skill;

/**
 * A human crew member. Holds health, fatigue and skills; subclasses define capabilities.
 *
 * @author A
 */
public abstract class CrewMember implements Agent {
    public static final int UNUSABLE_HEALTH_AT_OR_BELOW = 20;
    public static final int FATIGUE_LIMIT = 100;
    public static final int FATIGUE_PER_TICK = 10;
    public static final int REST_PER_TICK = 5;

    private final String id;
    private final String name;
    private final Location home;
    private Location location;
    private final Set<Skill> skills;
    private int health = 100;
    private int fatigue = 0;
    private boolean assigned = false;

    /**
     * Creates a crew member at full health, located at home.
     *
     * @param id    unique id
     * @param name  display name
     * @param home  starting and return location
     * @param skills the member's skills (copied)
     * @throws IllegalArgumentException if id/name is blank or home/skills is null
     */
    protected CrewMember(String id, String name, Location home, Set<Skill> skills) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        if (home == null) throw new IllegalArgumentException("home required");
        if (skills == null) throw new IllegalArgumentException("skills required");
        this.id = id;
        this.name = name;
        this.home = home;
        this.location = home;
        this.skills = EnumSet.noneOf(Skill.class);
        this.skills.addAll(skills);
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

    /** @return current health, 0..100 */
    public int getHealth() { return health; }

    /** @return current fatigue, 0..100 */
    public int getFatigue() { return fatigue; }

    /** @return unmodifiable set of skills */
    public Set<Skill> getSkills() { return Collections.unmodifiableSet(skills); }

    /**
     * Checks whether this member has a skill.
     *
     * @param skill the skill to look for
     * @return true if present
     */
    public boolean hasSkill(Skill skill) { return skills.contains(skill); }

    /** @return a short description of what this member does (polymorphism demo) */
    public abstract String performDuty();

    /** @return the standard "lacks capability" sentence */
    protected String lacksCapabilityReason() {
        return getName() + " does not possess the required capability";
    }

    @Override
    public String getUnusableReason() {
        if (health <= UNUSABLE_HEALTH_AT_OR_BELOW) {
            return name + " is too injured (health " + health + ")";
        }
        if (fatigue >= FATIGUE_LIMIT) {
            return name + " is exhausted";
        }
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
        health = Math.max(0, health - amount);
    }

    @Override
    public void recover(int amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        health = Math.min(100, health + amount);
    }

    @Override
    public void tick() {
        if (assigned) {
            fatigue = Math.min(FATIGUE_LIMIT, fatigue + FATIGUE_PER_TICK);
        } else {
            fatigue = Math.max(0, fatigue - REST_PER_TICK);
        }
    }

    @Override
    public String describe() {
        String state;
        if (!isUsable()) {
            state = "unusable";
        } else {
            state = "usable " + (assigned ? "assigned" : "available");
        }
        return name + " [" + getClass().getSimpleName() + "] @" + location.getName()
                + " health=" + health + " fatigue=" + fatigue + " " + state;
    }
}