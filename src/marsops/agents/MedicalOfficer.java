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
 * A medical officer: treats patients and responds to medical emergencies and fires.
 *
 * @author A
 */
public class MedicalOfficer extends CrewMember {
    private static final Set<Capability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(Capability.MEDICAL));

    /**
     * Creates a medical officer.
     *
     * @param id     unique id
     * @param name   display name
     * @param home   home location
     * @param skills the officer's skills
     */
    public MedicalOfficer(String id, String name, Location home, Set<Skill> skills) {
        super(id, name, home, skills);
    }

    @Override public Set<Capability> getCapabilities() { return CAPABILITIES; }

    @Override
    public String getCannotPerformReason(Mission mission) {
        if (!mission.getRequiredCapabilities().contains(Capability.MEDICAL)) {
            return lacksCapabilityReason();
        }
        return null;
    }

    @Override
    public boolean canHandle(EmergencyType type) {
        return type == EmergencyType.MEDICAL_EMERGENCY || type == EmergencyType.FIRE;
    }

    @Override public String performDuty() { return "Treating patients."; }
}