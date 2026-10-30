package marsops.missions;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.FacilityStatus;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.common.Skill;
import marsops.facilities.Facility;

/**
 * Repairs a damaged facility. When it completes, the target becomes OPERATIONAL again
 * (done in the {@link #onComplete()} hook).
 */
public class RepairMission extends AbstractMission {

    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR));

    private final Facility target;

    /**
     * Creates a repair mission.
     *
     * @param id            the mission id
     * @param target        the facility to repair (not null)
     * @param durationUnits duration in time units (at least 1)
     * @param resources     resources consumed on acceptance
     * @throws IllegalArgumentException if target is null or the duration is invalid
     */
    public RepairMission(String id, Facility target, int durationUnits,
                         ResourceRequirement resources) {
        super(id, MissionType.REPAIR, durationUnits, resources, listOf(target));
        this.target = target;
    }
    private static List<Facility> listOf(Facility target) {
        if (target == null) {
            throw new IllegalArgumentException("Target facility is required");
        }
        return Collections.singletonList(target);
    }
    @Override
    protected void onComplete() {
        target.setStatus(FacilityStatus.OPERATIONAL);
    }
    @Override
    public Set<Capability> getRequiredCapabilities() {
        return CAPABILITIES;
    }
    @Override
    public Skill getRequiredSkill() {
        return null;
    }
    @Override
    public int getMinBatteryPercent() {
        return 0;
    }
    @Override
    public List<String> getRequiredEquipment() {
        return Collections.emptyList();
    }
    @Override
    public boolean requiresCommunication() {
        return false;
    }
    @Override
    public boolean requiresSurface() {
        return false;
    }
    @Override
    public String getStartBlockReason(EnvironmentCondition env) {
        if (target.getStatus() == FacilityStatus.DESTROYED) {
            return "Facility " + target.getName() + " is destroyed and cannot be repaired";
        }
        return null;
    }
    @Override
    public String getInProgressFailureReason(EnvironmentCondition env) {
        return null;
    }
}