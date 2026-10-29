package marsops.facilities;

/**
 * A communication station: the link is up only while it is operational.
 *
 * @author A
 */
public class CommunicationStation extends Facility {
    /**
     * Creates a communication station.
     *
     * @param name     facility name
     * @param capacity maximum total units storable
     */
    public CommunicationStation(String name, int capacity) {
        super(name, capacity);
    }

    /** @return true if the station is operational */
    public boolean isLinkUp() { return isOperational(); }

    @Override public String getKind() { return "CommunicationStation"; }
}
