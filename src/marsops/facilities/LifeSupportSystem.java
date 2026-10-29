package marsops.facilities;

/**
 * The life-support system of a habitat. Owned by the habitat (composition).
 *
 * @author A
 */
public class LifeSupportSystem {
    private boolean functional = true;

    /** @return true if the system works */
    public boolean isFunctional() { return functional; }

    /**
     * Sets whether the system works.
     *
     * @param functional the new state
     */
    public void setFunctional(boolean functional) { this.functional = functional; }

    /** @return "Life support: functional" or "Life support: FAILED" */
    public String describe() {
        return functional ? "Life support: functional" : "Life support: FAILED";
    }
}
