package marsops.colony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import marsops.agents.Agent;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.Location;
import marsops.common.MartianSurface;
import marsops.common.MissionState;
import marsops.environment.Environment;
import marsops.facilities.CommunicationStation;
import marsops.facilities.Facility;
import marsops.missions.Mission;

/**
 * The colony: it owns the environment and the resource manager (composition), knows
 * its agents and facilities (aggregation), and decides whether a mission is accepted
 * and how time moves the active mission forward.
 */
public class MarsColony {

    private final String name;
    private final List<Agent> agents = new ArrayList<>();
    private final List<Facility> facilities = new ArrayList<>();
    private final ResourceManager resourceManager;
    private final Environment environment = new Environment();
    private Mission activeMission;
    private final List<Mission> missionHistory = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();

    /**
     * Creates a colony with a given allocation policy.
     *
     * @param name   the colony name (not blank)
     * @param policy how facilities supply resources (not null)
     * @throws IllegalArgumentException if the name is blank or the policy is null
     */
    public MarsColony(String name, AllocationPolicy policy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Colony name is required");
        }
        this.name = name;
        this.resourceManager = new ResourceManager(facilities, policy);
    }

    /**
     * Creates a colony that uses {@link StorageFirstPolicy}.
     *
     * @param name the colony name (not blank)
     */
    public MarsColony(String name) {
        this(name, new StorageFirstPolicy());
    }

    /** @return the colony name */
    public String getName() {
        return name;
    }

    /**
     * Adds an agent to the colony.
     *
     * @param agent the agent to add
     * @throws IllegalArgumentException if the agent is null or already in the colony
     */
    public void addAgent(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent is required");
        }
        if (agents.contains(agent) || findAgent(agent.getId()) != null) {
            throw new IllegalArgumentException("Agent already in colony: " + agent.getId());
        }
        agents.add(agent);
    }

    /**
     * Adds a facility to the colony. The order of addition matters for allocation.
     *
     * @param facility the facility to add
     * @throws IllegalArgumentException if it is null or its name is already used
     */
    public void addFacility(Facility facility) {
        if (facility == null) {
            throw new IllegalArgumentException("Facility is required");
        }
        if (findFacility(facility.getName()) != null) {
            throw new IllegalArgumentException("Facility already exists: " + facility.getName());
        }
        facilities.add(facility);
    }

    /** @return an unmodifiable view of the agents */
    public List<Agent> getAgents() {
        return Collections.unmodifiableList(agents);
    }

    /** @return an unmodifiable view of the facilities */
    public List<Facility> getFacilities() {
        return Collections.unmodifiableList(facilities);
    }

    /**
     * @param facilityName the facility name
     * @return the facility, or null if there is none with that name
     */
    public Facility findFacility(String facilityName) {
        for (Facility facility : facilities) {
            if (facility.getName().equals(facilityName)) {
                return facility;
            }
        }
        return null;
    }

    /**
     * @param agentId the agent id
     * @return the agent, or null if there is none with that id
     */
    public Agent findAgent(String agentId) {
        for (Agent agent : agents) {
            if (agent.getId().equals(agentId)) {
                return agent;
            }
        }
        return null;
    }

    /**
     * @param location a facility or the Martian surface
     * @return the agents currently at that location
     */
    public List<Agent> getAgentsAt(Location location) {
        List<Agent> result = new ArrayList<>();
        for (Agent agent : agents) {
            if (agent.getLocation() == location) {
                result.add(agent);
            }
        }
        return result;
    }

    /** @return true if at least one communication station is operational */
    public boolean hasOperationalCommunication() {
        for (Facility facility : facilities) {
            if (facility instanceof CommunicationStation
                    && ((CommunicationStation) facility).isLinkUp()) {
                return true;
            }
        }
        return false;
    }

    /** @return the environment */
    public Environment getEnvironment() {
        return environment;
    }

    /** @return the resource manager */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /** @return the mission now running, or null */
    public Mission getActiveMission() {
        return activeMission;
    }

    /** @return unmodifiable history of every accepted and rejected mission, in order */
    public List<Mission> getMissionHistory() {
        return Collections.unmodifiableList(missionHistory);
    }

    /**
     * Records a line in the event log.
     *
     * @param message the text to record
     */
    public void log(String message) {
        if (message != null) {
            eventLog.add(message);
        }
    }

    /** @return unmodifiable view of the event log */
    public List<String> getEventLog() {
        return Collections.unmodifiableList(eventLog);
    }

    // ------------------------------------------------------------------ submit

    /**
     * Tries to start a mission. The checks S0 to S8 run in a fixed order and the first
     * failing check rejects the mission. A rejected mission never becomes active and
     * consumes nothing.
     *
     * @param mission the mission to run (not null)
     * @param team    the agents proposed for it
     * @return accepted, or rejected with the reason
     * @throws IllegalArgumentException if the mission is null
     */
    public SubmissionResult submitMission(Mission mission, List<Agent> team) {
        if (mission == null) {
            throw new IllegalArgumentException("Mission is required");
        }
        if (mission.getState() != MissionState.CREATED) {                         // S0
            return reject(mission, "Mission was already processed", false);
        }
        String reason = firstRejectionReason(mission, team);                      // S1..S7
        if (reason != null) {
            return reject(mission, reason, true);
        }
        return accept(mission, team);                                             // S8
    }

    /** Runs S1 to S7 and returns the first reason, or null if the mission may be accepted. */
    private String firstRejectionReason(Mission mission, List<Agent> team) {
        if (activeMission != null) {                                              // S1
            return "Another mission is already active (" + activeMission.getId() + ")";
        }
        if (team == null || team.isEmpty()) {                                     // S2
            return "No agents were assigned to the mission";
        }
        String blocked = mission.getStartBlockReason(environment.getCondition()); // S3
        if (blocked != null) {
            return blocked;
        }
        if (mission.requiresCommunication() && !hasOperationalCommunication()) {  // S4
            return "No operational communication station";
        }
        String agentProblem = firstAgentProblem(mission, team);                   // S5
        if (agentProblem != null) {
            return agentProblem;
        }
        String missing = firstMissingCapability(mission, team);                   // S6
        if (missing != null) {
            return "No assigned agent provides capability " + missing;
        }
        return resourceManager.explainShortage(mission.getResourceRequirement()); // S7
    }

    /** S5: checks every agent in list order and returns the first problem, or null. */
    private String firstAgentProblem(Mission mission, List<Agent> team) {
        for (Agent agent : team) {
            if (agent == null) {
                return "The agent list contains an empty entry";
            }
            if (!agents.contains(agent)) {
                return agent.getName() + " is not part of this colony";
            }
            if (agent.isRemoteControlled() && environment.isCommunicationDisrupted()) {
                return agent.getName() + " cannot be controlled during a communication disruption";
            }
            String cannot = agent.getCannotPerformReason(mission);
            if (cannot != null) {
                return cannot;
            }
            if (!agent.isAvailable()) {
                String why = agent.isAssigned() ? "assigned to a mission" : agent.getUnusableReason();
                return agent.getName() + " is not available (" + why + ")";
            }
        }
        return null;
    }

    /** S6: returns the first required capability that no agent of the team provides, or null. */
    private String firstMissingCapability(Mission mission, List<Agent> team) {
        Set<Capability> covered = EnumSet.noneOf(Capability.class);
        for (Agent agent : team) {
            covered.addAll(agent.getCapabilities());
        }
        for (Capability required : mission.getRequiredCapabilities()) {
            if (!covered.contains(required)) {
                return required.toString();
            }
        }
        return null;
    }

    /** S8: consumes resources, assigns the team and makes the mission active. */
    private SubmissionResult accept(Mission mission, List<Agent> team) {
        resourceManager.consume(mission.getResourceRequirement());
        List<Agent> teamCopy = new ArrayList<>(team);
        mission.assign(teamCopy);
        for (Agent agent : teamCopy) {
            agent.assignToMission();
        }
        activeMission = mission;
        missionHistory.add(mission);
        log("Mission " + mission.getId() + " accepted with " + teamCopy.size() + " agent(s)");
        return SubmissionResult.accepted(mission);
    }

    /** Rejects a mission: optionally marks it FAILED, records it and returns the result. */
    private SubmissionResult reject(Mission mission, String reason, boolean markFailed) {
        if (markFailed) {
            mission.fail(reason);
        }
        missionHistory.add(mission);
        log("Mission " + mission.getId() + " rejected: " + reason);
        return SubmissionResult.rejected(mission, reason);
    }

    // -------------------------------------------------------------------- time

    /**
     * Fails the running mission (also used by the rover-breakdown emergency).
     *
     * @param reason why the mission fails (not blank)
     * @return false if there is no active mission, otherwise true
     */
    public boolean failActiveMission(String reason) {
        if (activeMission == null) {
            return false;
        }
        finishActiveMission(reason);
        return true;
    }

    /**
     * Changes the environment and applies rule D-06 to the running mission at once.
     *
     * @param condition the new condition (not null)
     * @throws IllegalArgumentException if condition is null
     */
    public void changeEnvironment(EnvironmentCondition condition) {
        environment.setCondition(condition);
        log("Environment changed to " + condition);
        if (activeMission != null) {
            String reason = activeMission.getInProgressFailureReason(condition);
            if (reason != null) {
                finishActiveMission(reason);
            }
        }
    }

    /**
     * Advances the colony by exactly one time unit: agents tick first, then the
     * active mission starts, checks for failure, or advances.
     */
    public void tick() {
        for (Agent agent : agents) {
            agent.tick();
        }
        if (activeMission == null) {
            return;
        }
        if (activeMission.getState() == MissionState.ASSIGNED) {
            activeMission.start();
            moveSurfaceAgentsToSurface(activeMission);
        }
        String reason = activeMission.getInProgressFailureReason(environment.getCondition());
        if (reason == null) {
            reason = firstUnusableReason(activeMission);
        }
        if (reason != null) {
            finishActiveMission(reason);
            return;
        }
        activeMission.advance();
        if (activeMission.getState() == MissionState.COMPLETED) {
            finishActiveMission(null);
        }
    }

    /** Surface missions move every agent that can travel on the surface out onto it. */
    private void moveSurfaceAgentsToSurface(Mission mission) {
        if (!mission.requiresSurface()) {
            return;
        }
        for (Agent agent : mission.getAssignedAgents()) {
            if (agent.hasCapability(Capability.SURFACE_TRAVEL)) {
                agent.setLocation(MartianSurface.INSTANCE);
            }
        }
    }

    /** @return the unusable reason of the first assigned agent that is unusable, or null */
    private String firstUnusableReason(Mission mission) {
        for (Agent agent : mission.getAssignedAgents()) {
            String reason = agent.getUnusableReason();
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    /**
     * Step 3 of the tick: ends the active mission. A non-null reason fails it; null means
     * it already COMPLETED. Agents are released and the usable ones go home (an
     * unusable agent, e.g. a damaged rover, stays where it is).
     */
    private void finishActiveMission(String failureReason) {
        Mission mission = activeMission;
        if (failureReason != null) {
            mission.fail(failureReason);
        }
        for (Agent agent : mission.getAssignedAgents()) {
            agent.release();
            if (agent.isUsable()) {
                agent.setLocation(agent.getHomeLocation());
            }
        }
        activeMission = null;
        if (failureReason != null) {
            log("Mission " + mission.getId() + " failed: " + failureReason);
        } else {
            log("Mission " + mission.getId() + " completed");
        }
    }
}