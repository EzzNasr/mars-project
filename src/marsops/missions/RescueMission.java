package marsops.missions;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.Location;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.common.Skill;
import marsops.facilities.Facility;

/**
 * Rescue of a stranded person or unit. It needs a vehicle and a medic, and it must be
 * coordinated by radio, so a communication disruption blocks or aborts it.
 */
public class RescueMission extends AbstractMission {

    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.VEHICLE, Capability.MEDICAL));

    private final String strandedEntity;
    private final Location strandedAt;
    private final int minBatteryPercent;

    /**
     * Creates a rescue mission.
     *
     * @param id                the mission id
     * @param strandedEntity    who or what is stranded (not blank)
     * @param strandedAt        where it is stranded (a facility or the Martian surface)
     * @param durationUnits     duration in time units (at least 1)
     * @param minBatteryPercent minimum battery a robot needs to take part
     * @param resources         resources consumed on acceptance
     * @throws IllegalArgumentException if the entity is blank, location is null
     *                                  or the duration is invalid
     */
    public RescueMission(String id, String strandedEntity, Location strandedAt,
                         int durationUnits, int minBatteryPercent,
                         ResourceRequirement resources) {
        super(id, MissionType.RESCUE, durationUnits, resources, targetsOf(strandedAt));
        if (strandedEntity == null || strandedEntity.isBlank()) {
            throw new IllegalArgumentException("Stranded entity is required");
        }
        this.strandedEntity = strandedEntity;
        this.strandedAt = strandedAt;
        this.minBatteryPercent = minBatteryPercent;
    }

    /** The target list holds the stranded location only if it is a Facility. */
    private static List<Facility> targetsOf(Location strandedAt) {
        if (strandedAt == null) {
            throw new IllegalArgumentException("Stranded location is required");
        }
        if (strandedAt instanceof Facility) {
            return Collections.singletonList((Facility) strandedAt);
        }
        return Collections.emptyList();
    }

    /** @return who or what is stranded */
    public String getStrandedEntity() {
        return strandedEntity;
    }

    /** @return where the entity is stranded */
    public Location getStrandedAt() {
        return strandedAt;
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
        return minBatteryPercent;
    }

    @Override
    public List<String> getRequiredEquipment() {
        return Collections.emptyList();
    }

    @Override
    public boolean requiresCommunication() {
        return true;
    }

    @Override
    public boolean requiresSurface() {
        return false;
    }

    @Override
    public String getStartBlockReason(EnvironmentCondition env) {
        if (env == EnvironmentCondition.COMMUNICATION_DISRUPTION) {
            return "Communication disruption: rescue cannot be coordinated";
        }
        return null;
    }

    @Override
    public String getInProgressFailureReason(EnvironmentCondition env) {
        if (env == EnvironmentCondition.COMMUNICATION_DISRUPTION) {
            return "Communication lost during rescue";
        }
        return null;
    }
}