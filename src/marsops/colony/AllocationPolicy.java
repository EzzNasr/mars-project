package marsops.colony;

import java.util.List;
import marsops.common.ResourceType;

public interface AllocationPolicy {

    /**
     * Order in which facilities supply 'type'. Input: operational facilities in
     * the order they were added.
     *
     * @param operationalFacilities
     * @param type
     * @return 
     */
    //List<Facility> order(List<Facility> operationalFacilities, ResourceType type);             //WAITING FOR A'S COMMIT
}
