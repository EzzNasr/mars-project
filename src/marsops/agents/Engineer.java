package marsops.agents;

import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.common.Skill;
import marsops.missions.Mission;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An engineer crew member responsible for repairing equipment and handling technical emergencies.
 *
 * @author A
 */
public class Engineer extends CrewMember {

    /**
     * Constructs a new Engineer.
     *
     * @param id     the unique identifier for the engineer
     * @param name   the name of the engineer
     * @param home   the home location of the engineer
     * @param skills the set of skills possessed by the engineer
     */
    public Engineer(String id, String name, Location home, Set<Skill> skills) {
        super(id, name, home, skills);
    }

    /**
     * Gets the capabilities of this engineer, which is strictly ENGINEERING.
     *
     * @return an unmodifiable set containing the ENGINEERING capability
     */
    @Override
    public Set<Capability> getCapabilities() {
        return Collections.unmodifiableSet(EnumSet.of(Capability.ENGINEERING));
    }

    /**
     * Determines if the engineer cannot perform the given mission.
     * The engineer can perform the mission if it requires the ENGINEERING capability.
     *
     * @param mission the mission to check
     * @return null if the engineer can perform the mission, otherwise the reason they cannot
     */
    @Override
    public String getCannotPerformReason(Mission mission) {
        if (mission.getRequiredCapabilities().contains(Capability.ENGINEERING)) {
            return null;
        }
        return lacksCapabilityReason();
    }

    /**
     * Checks if the engineer can handle a specific type of emergency.
     * Engineers can handle OXYGEN_LEAK, FIRE, and ROVER_BREAKDOWN.
     *
     * @param type the type of emergency
     * @return true if the emergency type is supported, false otherwise
     */
    @Override
    public boolean canHandle(EmergencyType type) {
        return type == EmergencyType.OXYGEN_LEAK ||
               type == EmergencyType.FIRE ||
               type == EmergencyType.ROVER_BREAKDOWN;
    }

    /**
     * Performs the primary duty of the engineer.
     *
     * @return a string describing the duty being performed
     */
    @Override
    public String performDuty() {
        return "Repairing equipment.";
    }

    /**
     * Simulates the engineer repairing equipment for a specific mission.
     *
     * @param mission the mission for which the repair is being conducted
     * @return a string describing the repair action and the associated mission
     */
    public String repair(Mission mission) {
        return getName() + " is repairing for mission " + mission.getId();
    }
}