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
import marsops.facilities.Laboratory;

/**
 * Research carried out inside a laboratory. It cannot start, and fails if running,
 * when the lab is not operational.
 */
public class ResearchMission extends AbstractMission {

    private static final Set<Capability> CAPABILITIES = Collections.unmodifiableSet(
            EnumSet.of(Capability.RESEARCH));

    private final Laboratory lab;
    private final Skill requiredSkill;
    private final List<String> requiredEquipment;

    /**
     * Creates a research mission.
     *
     * @param id                the mission id
     * @param lab               the laboratory where the work happens (not null)
     * @param requiredSkill     skill a human must have, or null
     * @param durationUnits     duration in time units (at least 1)
     * @param requiredEquipment names of equipment the lab must have working (may be null)
     * @param resources         resources consumed on acceptance
     * @throws IllegalArgumentException if lab is null or the duration is invalid
     */
    public ResearchMission(String id, Laboratory lab, Skill requiredSkill, int durationUnits,
                           List<String> requiredEquipment, ResourceRequirement resources) {
        super(id, MissionType.RESEARCH, durationUnits, resources, listOf(lab));
        this.lab = lab;
        this.requiredSkill = requiredSkill;
        this.requiredEquipment = (requiredEquipment == null)
                ? new ArrayList<>() : new ArrayList<>(requiredEquipment);
    }

    private static List<marsops.facilities.Facility> listOf(Laboratory lab) {
        if (lab == null) {
            throw new IllegalArgumentException("Laboratory is required");
        }
        return Collections.singletonList(lab);
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
        return 0;
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
        return false;
    }
    @Override
    public String getStartBlockReason(EnvironmentCondition env) {
        if (!lab.isOperational()) {
            return "Laboratory " + lab.getName() + " is not operational";
        }
        for (String equipmentName : requiredEquipment) {
            if (!lab.hasFunctionalEquipment(equipmentName)) {
                return "Laboratory " + lab.getName() + " lacks working equipment: "
                        + equipmentName;
            }
        }
        return null;
    }
    @Override
    public String getInProgressFailureReason(EnvironmentCondition env) {
        if (!lab.isOperational()) {
            return "Laboratory " + lab.getName() + " became unavailable";
        }
        return null;
    }
}