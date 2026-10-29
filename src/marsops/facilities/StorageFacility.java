package marsops.facilities;

/**
 * A storage facility: the first place resources are taken from (D-01).
 *
 * @author A
 */
public class StorageFacility extends Facility {
    /**
     * Creates a storage facility.
     *
     * @param name     facility name
     * @param capacity maximum total units storable
     */
    public StorageFacility(String name, int capacity) {
        super(name, capacity);
    }

    @Override public String getKind() { return "Storage"; }
}
