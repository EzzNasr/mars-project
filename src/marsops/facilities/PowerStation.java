package marsops.facilities;

/**
 * A power station with a fixed output.
 *
 * @author A
 */
public class PowerStation extends Facility {
    private final int outputUnits;

    /**
     * Creates a power station.
     *
     * @param name        facility name
     * @param capacity    maximum total units storable
     * @param outputUnits power output
     */
    public PowerStation(String name, int capacity, int outputUnits) {
        super(name, capacity);
        this.outputUnits = outputUnits;
    }

    /** @return the power output */
    public int getOutputUnits() { return outputUnits; }

    @Override public String getKind() { return "PowerStation"; }
}
