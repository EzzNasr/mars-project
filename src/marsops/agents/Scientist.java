package marsops.agents;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.common.Skill;
import marsops.missions.Mission;

/**
 * A scientist: performs research missions that match their skills.
 *
 * @author A
 */
public class Scientist extends CrewMember {
    private static final Set<Capability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(Capability.RESEARCH));

    /**
     * Creates a scientist.
     *
     * @param id     unique id
     * @param name   display name
     * @param home   home location
     * @param skills the scientist's skills
     */
    public Scientist(String id, String name, Location home, Set<Skill> skills) {
        super(id, name, home, skills);
    }

    @Override public Set<Capability> getCapabilities() { return CAPABILITIES; }

    @Override
    public String getCannotPerformReason(Mission mission) {
        if (!mission.getRequiredCapabilities().contains(Capability.RESEARCH)) {
            return lacksCapabilityReason();
        }
        Skill required = mission.getRequiredSkill();
        if (required != null && !hasSkill(required)) {
            return getName() + " lacks the required skill " + required;
        }
        return null;
    }

    @Override public boolean canHandle(EmergencyType type) { return false; }

    @Override public String performDuty() { return "Conducting research."; }
}