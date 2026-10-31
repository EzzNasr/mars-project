package marsops.colony;

import java.util.List;
import marsops.common.ResourceType;
import marsops.facilities.Facility;

/**
 * Strategy that decides in which order facilities supply a resource.
 * Different policies can be plugged into the {@link ResourceManager}.
 */
public interface AllocationPolicy {

    /**
     * Orders the facilities that should supply a resource.
     *
     * @param operationalFacilities operational facilities, in the order they were added
     * @param type                  the resource being withdrawn
     * @return a new list with the same facilities in the order they should be used
     */
    List<Facility> order(List<Facility> operationalFacilities, ResourceType type);
}