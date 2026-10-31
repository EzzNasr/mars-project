package marsops.colony;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Facility;

/**
 * Counts the colony's resources and withdraws them for missions.
 * It works on the colony's LIVE facility list, so facilities added later are seen at once.
 */
public class ResourceManager {

    private final List<Facility> facilities;
    private AllocationPolicy policy;

    /**
     * Creates a manager over a live list of facilities (the list is not copied).
     *
     * @param facilities the colony's facility list
     * @param policy     the allocation policy to use
     * @throws IllegalArgumentException if either argument is null
     */
    public ResourceManager(List<Facility> facilities, AllocationPolicy policy) {
        if (facilities == null || policy == null) {
            throw new IllegalArgumentException("Facilities and policy are required");
        }
        this.facilities = facilities;
        this.policy = policy;
    }

    /**
     * Replaces the allocation policy.
     *
     * @param policy the new policy (not null)
     * @throws IllegalArgumentException if policy is null
     */
    public void setPolicy(AllocationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy is required");
        }
        this.policy = policy;
    }

    /**
     * Colony-wide total of a resource (D-02): only OPERATIONAL facilities count.
     *
     * @param type the resource type
     * @return the sum of available units over all facilities
     */
    public int getTotal(ResourceType type) {
        int total = 0;
        for (Facility facility : facilities) {
            total += facility.getAvailable(type);
        }
        return total;
    }

    /**
     * @return the total of every resource type, in enum order
     */
    public Map<ResourceType, Integer> getAllTotals() {
        Map<ResourceType, Integer> totals = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            totals.put(type, getTotal(type));
        }
        return totals;
    }

    /**
     * @param requirement what is needed
     * @return true if every needed resource is available in the colony
     */
    public boolean canSupply(ResourceRequirement requirement) {
        return explainShortage(requirement) == null;
    }

    /**
     * Explains the first shortage, checking types in enum order.
     *
     * @param requirement what is needed
     * @return null if everything is available; otherwise e.g.
     *         "Insufficient OXYGEN: need 600, available 550"
     */
    public String explainShortage(ResourceRequirement requirement) {
        if (requirement == null) {
            return null;
        }
        for (ResourceType type : ResourceType.values()) {
            int needed = requirement.get(type);
            int available = getTotal(type);
            if (needed > 0 && available < needed) {
                return "Insufficient " + type + ": need " + needed + ", available " + available;
            }
        }
        return null;
    }

    /**
     * Withdraws the requirement, all-or-nothing (D-03): if anything is short,
     * nothing at all is changed.
     *
     * @param requirement what to withdraw
     * @return true if consumed; false if there was a shortage
     */
    public boolean consume(ResourceRequirement requirement) {
        if (requirement == null) {
            return true;
        }
        if (explainShortage(requirement) != null) {
            return false;
        }
        for (ResourceType type : ResourceType.values()) {
            int remaining = requirement.get(type);
            if (remaining > 0) {
                withdrawFromFacilities(type, remaining);
            }
        }
        return true;
    }

    /** Takes 'amount' of 'type' from facilities in the order chosen by the policy. */
    private void withdrawFromFacilities(ResourceType type, int amount) {
        List<Facility> operational = new ArrayList<>();
        for (Facility facility : facilities) {
            if (facility.isOperational()) {
                operational.add(facility);
            }
        }
        int remaining = amount;
        for (Facility facility : policy.order(operational, type)) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, facility.getStored(type));
            if (take > 0 && facility.withdraw(type, take)) {
                remaining -= take;
            }
        }
    }
}