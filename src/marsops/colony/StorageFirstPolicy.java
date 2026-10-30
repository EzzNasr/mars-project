package marsops.colony;

import java.util.ArrayList;
import java.util.List;
import marsops.common.ResourceType;
import marsops.facilities.Facility;
import marsops.facilities.StorageFacility;

/**
 * Rule D-01: storage facilities supply first, then all other facilities.
 * The original order is kept inside each group.
 */
public class StorageFirstPolicy implements AllocationPolicy {

    @Override
    public List<Facility> order(List<Facility> operationalFacilities, ResourceType type) {
        List<Facility> storages = new ArrayList<>();
        List<Facility> others = new ArrayList<>();
        for (Facility facility : operationalFacilities) {
            if (facility instanceof StorageFacility) {
                storages.add(facility);
            } else {
                others.add(facility);
            }
        }
        List<Facility> result = new ArrayList<>(storages);
        result.addAll(others);
        return result;
    }
}