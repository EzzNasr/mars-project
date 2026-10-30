package marsops.missions;

import java.util.List;
import java.util.Set;
import marsops.agents.Agent;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.MissionState;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.common.Skill;
import marsops.facilities.Facility;

/**
 * A task the colony can run: it asks for capabilities and resources, and moves
 * through the lifecycle CREATED, ASSIGNED, IN_PROGRESS, COMPLETED or FAILED.
 *
 */
public interface Mission {

    String getId();
    MissionType getType();
    MissionState getState();
    int getDurationUnits();
    int getProgressUnits();

    /** @return the failure reason, or null unless the mission is FAILED */
    String getFailureReason();

    /** @return unmodifiable list of agents; empty until the mission is ASSIGNED */
    List<Agent> getAssignedAgents();

    /** @return unmodifiable list of target facilities (may be empty) */
    List<Facility> getTargetFacilities();

    ResourceRequirement getResourceRequirement();

    // --- what agents and the colony ask ---

    /** @return the capabilities the whole team must cover */
    Set<Capability> getRequiredCapabilities();

    /** @return the skill a human must have, or null if none */
    Skill getRequiredSkill();

    /** @return minimum robot battery percent, 0 if none */
    int getMinBatteryPercent();

    /** @return names of required equipment (may be empty) */
    List<String> getRequiredEquipment();

    boolean requiresCommunication();
    boolean requiresSurface();

    /**
     * @param env the current environment condition
     * @return null if the mission may start now; otherwise the reason it cannot
     */
    String getStartBlockReason(EnvironmentCondition env);

    /**
     * @param env the current environment condition
     * @return null if a running mission may continue; otherwise why it must fail
     */
    String getInProgressFailureReason(EnvironmentCondition env);

    // --- lifecycle: called ONLY by MarsColony ---

    void assign(List<Agent> agents);   // CREATED -> ASSIGNED
    void start();                      // ASSIGNED -> IN_PROGRESS
    void advance();                    // IN_PROGRESS: progress + 1, maybe COMPLETED
    void fail(String reason);          // CREATED / ASSIGNED / IN_PROGRESS -> FAILED
    boolean isFinished();              // COMPLETED or FAILED
    String describe();                 // "M-1 [EXPLORATION] IN_PROGRESS 2/3"
}