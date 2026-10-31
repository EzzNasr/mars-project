package marsops.missions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.common.Skill;

/**
 * Exploration of a region of Mars. Needs surface travel, so a dust storm blocks it
 * and aborts it if it is already running.
 */
public class ExplorationMission extends AbstractMission {

    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.RESEARCH, Capability.SURFACE_TRAVEL));

    private final String regionName;
    private final int minBatteryPercent;
    private final Skill requiredSkill;
    private final List<String> requiredEquipment;

    /**
     * Creates an exploration mission.
     *
     * @param id                the mission id
     * @param regionName        the region to explore (not blank)
     * @param durationUnits     duration in time units (at least 1)
     * @param minBatteryPercent minimum battery a robot needs to take part
     * @param requiredSkill     skill a human must have, or null
     * @param requiredEquipment names of equipment the rover must carry (may be null)
     * @param resources         resources consumed on acceptance
     * @throws IllegalArgumentException if the region is blank or the duration is invalid
     */
    public ExplorationMission(String id, String regionName, int durationUnits,
                              int minBatteryPercent, Skill requiredSkill,
                              List<String> requiredEquipment, ResourceRequirement resources) {
        super(id, MissionType.EXPLORATION, durationUnits, resources, null);
        if (regionName == null || regionName.isBlank()) {
            throw new IllegalArgumentException("Region name is required");
        }
        this.regionName = regionName;
        this.minBatteryPercent = minBatteryPercent;
        this.requiredSkill = requiredSkill;
        this.requiredEquipment = (requiredEquipment == null)
                ? new ArrayList<>() : new ArrayList<>(requiredEquipment);
    }

    /** @return the name of the region to explore */
    public String getRegionName() {
        return regionName;
    }

    @Override
    public Set<Capability> getRequiredCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public Skill getRequiredSkill() {
        return requiredSkill;
    }

    @Override
    public int getMinBatteryPercent() {
        return minBatteryPercent;
    }

    @Override
    public List<String> getRequiredEquipment() {
        return Collections.unmodifiableList(requiredEquipment);
    }

    @Override
    public boolean requiresCommunication() {
        return false;
    }

    @Override
    public boolean requiresSurface() {
        return true;
    }

    @Override
    public String getStartBlockReason(EnvironmentCondition env) {
        if (env == EnvironmentCondition.DUST_STORM) {
            return "Surface operations are not allowed during a dust storm";
        }
        return null;
    }

    @Override
    public String getInProgressFailureReason(EnvironmentCondition env) {
        if (env == EnvironmentCondition.DUST_STORM) {
            return "Dust storm: active rover mission aborted";
        }
        return null;
    }
}