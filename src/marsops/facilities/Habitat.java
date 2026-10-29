package marsops.facilities;

import marsops.common.ResourceType;

/**
 * A habitat: a facility where the crew live. It owns its life-support system (composition).
 *
 * @author A
 */
public class Habitat extends Facility {
    private final LifeSupportSystem lifeSupport;
    private boolean leaking = false;

    /**
     * Creates a habitat with its own life-support system.
     *
     * @param name     facility name
     * @param capacity maximum total units storable
     */
    public Habitat(String name, int capacity) {
        super(name, capacity);
        this.lifeSupport = new LifeSupportSystem();
    }

    /** @return this habitat's life-support system (always the same object) */
    public LifeSupportSystem getLifeSupport() { return lifeSupport; }

    /** @return true while an oxygen leak is active */
    public boolean isLeaking() { return leaking; }

    /**
     * Sets the leak flag.
     *
     * @param leaking true if the habitat is leaking
     */
    public void setLeaking(boolean leaking) { this.leaking = leaking; }

    /**
     * Loses oxygen, never going below zero.
     *
     * @param amount units to lose
     * @return units actually lost
     */
    public int loseOxygen(int amount) { return lose(ResourceType.OXYGEN, amount); }

    @Override public String getKind() { return "Habitat"; }
}
