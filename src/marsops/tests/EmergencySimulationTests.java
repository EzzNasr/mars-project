package marsops.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import marsops.Main;
import marsops.agents.Agent; // waiting for A's commit
import marsops.agents.CrewMember; // waiting for A's commit
import marsops.agents.Engineer; // waiting for A's commit
import marsops.agents.ExplorationRover; // waiting for A's commit
import marsops.agents.RepairRobot; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.colony.SubmissionResult; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyState;
import marsops.common.EmergencyType;
import marsops.common.EnvironmentCondition;
import marsops.common.FacilityStatus;
import marsops.common.MartianSurface;
import marsops.common.MissionState;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.common.RobotCondition;
import marsops.common.Skill;
import marsops.emergencies.Emergency;
import marsops.emergencies.Fire;
import marsops.emergencies.MedicalEmergency;
import marsops.emergencies.OxygenLeak;
import marsops.emergencies.RoverBreakdown;
import marsops.facilities.Habitat; // waiting for A's commit
import marsops.facilities.Laboratory; // waiting for A's commit
import marsops.facilities.StorageFacility; // waiting for A's commit
import marsops.missions.ExplorationMission; // waiting for B's commit
import marsops.missions.RepairMission; // waiting for B's commit
import marsops.simulation.Simulation;

// Tests for part C: emergencies, simulation and the Main scenarios (C01 - C20).
// It uses the sample colony from Main.buildSampleSimulation().
public class EmergencySimulationTests {

    public static void main(String[] args) {
        c01_requirementsMatchRules();
        c02_oxygenLeakDetection();
        c03_oxygenLeakRepaired();
        c04_noEngineerAvailable();
        c05_notEnoughSpareParts();
        c06_scientistIsNeverDispatched();
        c07_busyResponderIsSkipped();
        c08_emergencyDoesNotInterruptMission();
        c09_fire();
        c10_roverBreakdown();
        c11_medicalEmergency();
        c12_advanceTime();
        c13_scheduledEnvironmentChange();
        c14_scheduledEmergency();
        c15_triggerEmergency();
        c16_historyAndUnresolved();
        c17_to_c20_mainScenarios();
        Check.summary();
    }

    // ---------- C01 ----------
    private static void c01_requirementsMatchRules() {
        Check.section("C01 requirements of the 4 emergencies (D-10 / D-11)");
        MarsColony colony = Main.buildSampleSimulation().getColony();
        Habitat habitat = habitat(colony);
        Laboratory lab = (Laboratory) colony.findFacility("Lab 1");
        ExplorationRover rover = (ExplorationRover) colony.findAgent("R-01");
        CrewMember patient = (CrewMember) colony.findAgent("H-01");

        checkRules("C01 oxygen leak", new OxygenLeak("E-1", habitat, 80), EmergencyType.OXYGEN_LEAK,
                Arrays.asList(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR),
                new ResourceRequirement().with(ResourceType.SPARE_PARTS, 2).with(ResourceType.BATTERY_POWER, 5));
        checkRules("C01 fire", new Fire("E-2", lab), EmergencyType.FIRE,
                Arrays.asList(Capability.ENGINEERING, Capability.MEDICAL),
                new ResourceRequirement().with(ResourceType.WATER, 10).with(ResourceType.MEDICAL_SUPPLIES, 1)
                        .with(ResourceType.BATTERY_POWER, 3));
        checkRules("C01 rover breakdown", new RoverBreakdown("E-3", rover), EmergencyType.ROVER_BREAKDOWN,
                Arrays.asList(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR),
                new ResourceRequirement().with(ResourceType.SPARE_PARTS, 2).with(ResourceType.BATTERY_POWER, 5));
        checkRules("C01 medical emergency", new MedicalEmergency("E-4", patient, 10),
                EmergencyType.MEDICAL_EMERGENCY, Arrays.asList(Capability.MEDICAL),
                new ResourceRequirement().with(ResourceType.MEDICAL_SUPPLIES, 2).with(ResourceType.OXYGEN, 5));
    }

    private static void checkRules(String name, Emergency e, EmergencyType type, List<Capability> caps,
            ResourceRequirement resources) {
        Check.equal(name + " type", type, e.getType());
        Check.equal(name + " responder capabilities", caps, e.getRequiredResponderCapabilities());
        Check.equal(name + " response resources", resources.asMap(), e.getResponseResources().asMap());
    }

    // ---------- C02 ----------
    private static void c02_oxygenLeakDetection() {
        Check.section("C02 oxygen leak loses oxygen, never below 0");
        Simulation sim = Main.buildSampleSimulation();
        Habitat habitat = habitat(sim.getColony());
        sim.triggerEmergency(new OxygenLeak("E-1", habitat, 80));
        Check.equal("C02 habitat 300 with loss 80 -> 220", 220, habitat.getStored(ResourceType.OXYGEN));

        Simulation sim2 = Main.buildSampleSimulation();
        Habitat habitat2 = habitat(sim2.getColony());
        sim2.triggerEmergency(new OxygenLeak("E-1", habitat2, 1000));
        Check.equal("C02 loss 1000 on 300 -> 0", 0, habitat2.getStored(ResourceType.OXYGEN));
    }

    // ---------- C03 ----------
    private static void c03_oxygenLeakRepaired() {
        Check.section("C03 oxygen leak repaired");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Habitat habitat = habitat(colony);
        int oxygenBefore = total(colony, ResourceType.OXYGEN);
        int sparesBefore = total(colony, ResourceType.SPARE_PARTS);
        int batteryBefore = total(colony, ResourceType.BATTERY_POWER);

        OxygenLeak leak = new OxygenLeak("E-1", habitat, 80);
        sim.triggerEmergency(leak);

        Check.equal("C03 state RESOLVED", EmergencyState.RESOLVED, leak.getState());
        Check.equal("C03 total oxygen loss is exactly 80", oxygenBefore - 80, total(colony, ResourceType.OXYGEN));
        Check.isTrue("C03 habitat is not leaking", !habitat.isLeaking());
        Check.equal("C03 SPARE_PARTS -2", sparesBefore - 2, total(colony, ResourceType.SPARE_PARTS));
        Check.equal("C03 BATTERY_POWER -5", batteryBefore - 5, total(colony, ResourceType.BATTERY_POWER));
        Check.equal("C03 two responders", 2, leak.getResponders().size());
        for (Agent responder : leak.getResponders()) {
            Check.isTrue("C03 " + responder.getName() + " is released", !responder.isAssigned());
            Check.isTrue("C03 " + responder.getName() + " is at the habitat", responder.getLocation() == habitat);
        }
    }

    // ---------- C04 ----------
    private static void c04_noEngineerAvailable() {
        Check.section("C04 no engineer available");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Habitat habitat = habitat(colony);
        busyOmarAndFixer(colony, "Fixer-1 busy", "H-02", "R-02");
        int sparesBefore = total(colony, ResourceType.SPARE_PARTS);
        int batteryBefore = total(colony, ResourceType.BATTERY_POWER);

        OxygenLeak leak = new OxygenLeak("E-1", habitat, 80);
        sim.triggerEmergency(leak);

        Check.equal("C04 state UNRESOLVED", EmergencyState.UNRESOLVED, leak.getState());
        Check.contains("C04 reason mentions ENGINEERING", leak.getReason(), "ENGINEERING");
        Check.equal("C04 oxygen loss is exactly 160", 140, habitat.getStored(ResourceType.OXYGEN));
        Check.equal("C04 spare parts unchanged", sparesBefore, total(colony, ResourceType.SPARE_PARTS));
        Check.equal("C04 battery unchanged", batteryBefore, total(colony, ResourceType.BATTERY_POWER));
        Check.equal("C04 getUnresolved size is 1", 1, sim.getEmergencyManager().getUnresolved().size());

        sim.advanceTime(2);
        Check.equal("C04 nothing retried: history size", 1, sim.getEmergencyManager().getHistory().size());
        Check.equal("C04 nothing retried: oxygen unchanged", 140, habitat.getStored(ResourceType.OXYGEN));
    }

    // ---------- C05 ----------
    private static void c05_notEnoughSpareParts() {
        Check.section("C05 not enough spare parts");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Habitat habitat = habitat(colony);
        StorageFacility storage = (StorageFacility) colony.findFacility("Storage");
        storage.withdraw(ResourceType.SPARE_PARTS, 9); // only 1 left, the leak needs 2
        int batteryBefore = total(colony, ResourceType.BATTERY_POWER);

        OxygenLeak leak = new OxygenLeak("E-1", habitat, 80);
        sim.triggerEmergency(leak);

        Check.equal("C05 state UNRESOLVED", EmergencyState.UNRESOLVED, leak.getState());
        Check.contains("C05 reason mentions SPARE_PARTS", leak.getReason(), "SPARE_PARTS");
        Check.equal("C05 spare parts not consumed", 1, total(colony, ResourceType.SPARE_PARTS));
        Check.equal("C05 battery not consumed", batteryBefore, total(colony, ResourceType.BATTERY_POWER));
        Check.equal("C05 oxygen loss is 160", 140, habitat.getStored(ResourceType.OXYGEN));
    }

    // ---------- C06 ----------
    private static void c06_scientistIsNeverDispatched() {
        Check.section("C06 responder selection uses canHandle");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        colony.findAgent("H-02").takeDamage(100); // Omar out of action
        colony.findAgent("H-03").takeDamage(100); // Salma out of action
        Agent layla = colony.findAgent("H-01"); // the only free human is a Scientist

        OxygenLeak leak = new OxygenLeak("E-1", habitat(colony), 80);
        sim.triggerEmergency(leak);

        Check.equal("C06 state UNRESOLVED", EmergencyState.UNRESOLVED, leak.getState());
        Check.equal("C06 no responders", 0, leak.getResponders().size());
        Check.isTrue("C06 scientist was not assigned", !layla.isAssigned());
        Check.isTrue("C06 scientist stayed at home", layla.getLocation() == layla.getHomeLocation());
    }

    // ---------- C07 ----------
    private static void c07_busyResponderIsSkipped() {
        Check.section("C07 busy responder is skipped");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Habitat habitat = habitat(colony);
        Engineer nadia = new Engineer("H-04", "Eng. Nadia", habitat, EnumSet.of(Skill.ENGINEERING));
        RepairRobot fixer2 = new RepairRobot("R-04", "Fixer-2", colony.findFacility("Storage"));
        colony.addAgent(nadia);
        colony.addAgent(fixer2);
        Agent omar = colony.findAgent("H-02");
        busyOmarAndFixer(colony, "Fixer-2 busy", "H-02", "R-04");

        OxygenLeak leak = new OxygenLeak("E-1", habitat, 80);
        sim.triggerEmergency(leak);

        Check.equal("C07 state RESOLVED", EmergencyState.RESOLVED, leak.getState());
        Check.isTrue("C07 second engineer was chosen", leak.getResponders().contains(nadia));
        Check.isTrue("C07 busy engineer was skipped", !leak.getResponders().contains(omar));
        Check.isTrue("C07 busy engineer is still assigned to the mission", omar.isAssigned());
    }

    // ---------- C08 ----------
    private static void c08_emergencyDoesNotInterruptMission() {
        Check.section("C08 emergency does not interrupt the active mission");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ExplorationMission mission = explorationMission();
        colony.submitMission(mission, team(colony.findAgent("H-01"), colony.findAgent("R-01")));
        sim.advanceTime(1);
        Check.equal("C08 mission is IN_PROGRESS before", MissionState.IN_PROGRESS, mission.getState());

        sim.triggerEmergency(new OxygenLeak("E-1", habitat(colony), 80));
        Check.equal("C08 mission is still IN_PROGRESS", MissionState.IN_PROGRESS, mission.getState());
    }

    // ---------- C09 ----------
    private static void c09_fire() {
        Check.section("C09 fire");
        // resolved
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Laboratory lab = (Laboratory) colony.findFacility("Lab 1");
        Agent layla = colony.findAgent("H-01");
        layla.setLocation(lab); // somebody is inside the lab when it burns

        Fire fire = new Fire("E-2", lab);
        sim.triggerEmergency(fire);

        Check.equal("C09 state RESOLVED", EmergencyState.RESOLVED, fire.getState());
        Check.equal("C09 lab is DAMAGED", FacilityStatus.DAMAGED, lab.getStatus());
        Check.isTrue("C09 fire is out", !lab.isOnFire());
        Check.isTrue("C09 equipment is damaged", !lab.hasFunctionalEquipment("Microscope"));
        Check.equal("C09 agent in the lab: -30 then +20 = 90", 90, ((CrewMember) layla).getHealth());

        // unresolved (no medical officer)
        Simulation sim2 = Main.buildSampleSimulation();
        MarsColony colony2 = sim2.getColony();
        Laboratory lab2 = (Laboratory) colony2.findFacility("Lab 1");
        colony2.findAgent("H-03").takeDamage(100); // Salma out of action

        Fire fire2 = new Fire("E-2", lab2);
        sim2.triggerEmergency(fire2);

        Check.equal("C09 unresolved: state UNRESOLVED", EmergencyState.UNRESOLVED, fire2.getState());
        Check.equal("C09 unresolved: lab DESTROYED", FacilityStatus.DESTROYED, lab2.getStatus());
        Check.equal("C09 unresolved: lab stock no longer counts (300 + 200)", 500,
                total(colony2, ResourceType.OXYGEN));
    }

    // ---------- C10 ----------
    private static void c10_roverBreakdown() {
        Check.section("C10 rover breakdown");
        // unresolved: the repair robot is damaged, so the rover stays where it broke down
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ExplorationRover rover = (ExplorationRover) colony.findAgent("R-01");
        ExplorationMission mission = explorationMission();
        colony.submitMission(mission, team(colony.findAgent("H-01"), rover));
        sim.advanceTime(1);
        colony.findAgent("R-02").takeDamage(30); // Fixer-1 is damaged

        RoverBreakdown breakdown = new RoverBreakdown("E-3", rover);
        sim.triggerEmergency(breakdown);

        Check.equal("C10 rover is DAMAGED", RobotCondition.DAMAGED, rover.getCondition());
        Check.isTrue("C10 rover is still on the surface", rover.getLocation() == MartianSurface.INSTANCE);
        Check.equal("C10 mission FAILED", MissionState.FAILED, mission.getState());
        Check.contains("C10 mission reason mentions broke down", mission.getFailureReason(), "broke down");
        Check.equal("C10 no repair robot: UNRESOLVED", EmergencyState.UNRESOLVED, breakdown.getState());

        // resolved: responders go to the surface, then the rover is repaired and sent home
        Simulation sim2 = Main.buildSampleSimulation();
        MarsColony colony2 = sim2.getColony();
        ExplorationRover rover2 = (ExplorationRover) colony2.findAgent("R-01");
        colony2.submitMission(explorationMission(), team(colony2.findAgent("H-01"), rover2));
        sim2.advanceTime(1);

        RoverBreakdown breakdown2 = new RoverBreakdown("E-3", rover2);
        sim2.triggerEmergency(breakdown2);

        Check.equal("C10 resolved: state RESOLVED", EmergencyState.RESOLVED, breakdown2.getState());
        for (Agent responder : breakdown2.getResponders()) {
            Check.isTrue("C10 " + responder.getName() + " was dispatched to the surface",
                    responder.getLocation() == MartianSurface.INSTANCE);
        }
        Check.equal("C10 rover is OPERATIONAL again", RobotCondition.OPERATIONAL, rover2.getCondition());
        Check.isTrue("C10 rover is at home", rover2.getLocation() == rover2.getHomeLocation());
    }

    // ---------- C11 ----------
    private static void c11_medicalEmergency() {
        Check.section("C11 medical emergency");
        // resolved
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        CrewMember layla = (CrewMember) colony.findAgent("H-01");
        int suppliesBefore = total(colony, ResourceType.MEDICAL_SUPPLIES);
        int oxygenBefore = total(colony, ResourceType.OXYGEN);

        MedicalEmergency emergency = new MedicalEmergency("E-4", layla, 50);
        sim.triggerEmergency(emergency);

        Check.equal("C11 state RESOLVED", EmergencyState.RESOLVED, emergency.getState());
        Check.equal("C11 health 100 -50 +40 = 90 (lower than before, higher than after the damage)", 90,
                layla.getHealth());
        Check.equal("C11 MEDICAL_SUPPLIES -2", suppliesBefore - 2, total(colony, ResourceType.MEDICAL_SUPPLIES));
        Check.equal("C11 OXYGEN -5", oxygenBefore - 5, total(colony, ResourceType.OXYGEN));

        // no medical officer
        Simulation sim2 = Main.buildSampleSimulation();
        sim2.getColony().findAgent("H-03").takeDamage(100);
        MedicalEmergency noMedic = new MedicalEmergency("E-4", (CrewMember) sim2.getColony().findAgent("H-01"), 20);
        sim2.triggerEmergency(noMedic);
        Check.equal("C11 no medic: UNRESOLVED", EmergencyState.UNRESOLVED, noMedic.getState());

        // facility not operational
        Simulation sim3 = Main.buildSampleSimulation();
        MarsColony colony3 = sim3.getColony();
        habitat(colony3).setStatus(FacilityStatus.DAMAGED);
        MedicalEmergency noFacility = new MedicalEmergency("E-4", (CrewMember) colony3.findAgent("H-01"), 20);
        sim3.triggerEmergency(noFacility);
        Check.equal("C11 facility not operational: UNRESOLVED", EmergencyState.UNRESOLVED, noFacility.getState());
        Check.contains("C11 reason says not operational", noFacility.getReason(), "not operational");

        // the patient is a medical officer and is the only one
        Simulation sim4 = Main.buildSampleSimulation();
        CrewMember salma = (CrewMember) sim4.getColony().findAgent("H-03");
        MedicalEmergency selfTreat = new MedicalEmergency("E-4", salma, 20);
        sim4.triggerEmergency(selfTreat);
        Check.equal("C11 medic patient: UNRESOLVED", EmergencyState.UNRESOLVED, selfTreat.getState());
        Check.isTrue("C11 patient is never their own responder", !selfTreat.getResponders().contains(salma));
    }

    // ---------- C12 ----------
    private static void c12_advanceTime() {
        Check.section("C12 advanceTime");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ExplorationMission mission = explorationMission();
        colony.submitMission(mission, team(colony.findAgent("H-01"), colony.findAgent("R-01")));

        sim.advanceTime(3);
        Check.equal("C12 current time is 3", 3, sim.getCurrentTime());
        Check.equal("C12 3-unit mission is COMPLETED", MissionState.COMPLETED, mission.getState());
        Check.throwsException("C12 advanceTime(-1) throws", IllegalArgumentException.class,
                () -> sim.advanceTime(-1));
    }

    // ---------- C13 ----------
    private static void c13_scheduledEnvironmentChange() {
        Check.section("C13 scheduled environment change");
        Simulation sim = Main.buildSampleSimulation();
        sim.scheduleEnvironmentChange(2, EnvironmentCondition.DUST_STORM);

        sim.advanceTime(1);
        Check.equal("C13 after 1 unit: NORMAL", EnvironmentCondition.NORMAL,
                sim.getColony().getEnvironment().getCondition());
        sim.advanceTime(1);
        Check.equal("C13 after 2 units: DUST_STORM", EnvironmentCondition.DUST_STORM,
                sim.getColony().getEnvironment().getCondition());
        sim.advanceTime(1);
        Check.equal("C13 after 3 units: still DUST_STORM", EnvironmentCondition.DUST_STORM,
                sim.getColony().getEnvironment().getCondition());
    }

    // ---------- C14 ----------
    private static void c14_scheduledEmergency() {
        Check.section("C14 scheduled emergency");
        Simulation sim = Main.buildSampleSimulation();
        Habitat habitat = habitat(sim.getColony());
        sim.scheduleEmergency(2, new OxygenLeak("E-1", habitat, 80));

        sim.advanceTime(1);
        Check.equal("C14 after 1 unit: nothing happened", 300, habitat.getStored(ResourceType.OXYGEN));
        sim.advanceTime(1);
        Check.equal("C14 after 2 units: habitat lost oxygen", 220, habitat.getStored(ResourceType.OXYGEN));
        sim.advanceTime(3);
        Check.equal("C14 after 5 units: handled exactly once", 1, sim.getEmergencyManager().getHistory().size());
        Check.equal("C14 oxygen was not lost again", 220, habitat.getStored(ResourceType.OXYGEN));
    }

    // ---------- C15 ----------
    private static void c15_triggerEmergency() {
        Check.section("C15 triggerEmergency");
        Simulation sim = Main.buildSampleSimulation();
        sim.triggerEmergency(new OxygenLeak("E-1", habitat(sim.getColony()), 80));
        Check.equal("C15 handled immediately (history size 1)", 1, sim.getEmergencyManager().getHistory().size());
        Check.equal("C15 time did not move", 0, sim.getCurrentTime());
    }

    // ---------- C16 ----------
    private static void c16_historyAndUnresolved() {
        Check.section("C16 history order and unresolved list");
        Simulation sim = Main.buildSampleSimulation();
        MarsColony colony = sim.getColony();
        OxygenLeak first = new OxygenLeak("E-1", habitat(colony), 80);
        // the only medical officer is the patient, so this one cannot be solved
        MedicalEmergency second = new MedicalEmergency("E-2", (CrewMember) colony.findAgent("H-03"), 20);
        sim.triggerEmergency(first);
        sim.triggerEmergency(second);

        List<Emergency> history = sim.getEmergencyManager().getHistory();
        Check.equal("C16 history size", 2, history.size());
        Check.isTrue("C16 history keeps the order", history.get(0) == first && history.get(1) == second);
        List<Emergency> unresolved = sim.getEmergencyManager().getUnresolved();
        Check.equal("C16 unresolved size", 1, unresolved.size());
        Check.isTrue("C16 unresolved contains only the UNRESOLVED one", unresolved.get(0) == second);
    }

    // ---------- C17 - C20 ----------
    private static void c17_to_c20_mainScenarios() {
        Check.section("C17 - C20 Main scenarios");
        Check.isTrue("C17 scenario 1 returns true", Main.scenario1_successfulExploration());
        Check.isTrue("C18 scenario 2 returns true", Main.scenario2_missionRejection());
        Check.isTrue("C19 scenario 3 returns true", Main.scenario3_resourceShortage());
        Check.isTrue("C20 scenario 4 returns true", Main.scenario4_emergencyHandling());
        Check.isTrue("C20 scenario 5 returns true", Main.scenario5_environmentalEffect());
    }

    // ---------- helpers ----------

    private static Habitat habitat(MarsColony colony) {
        return (Habitat) colony.findFacility("Habitat A");
    }

    private static int total(MarsColony colony, ResourceType type) {
        return colony.getResourceManager().getTotal(type);
    }

    private static List<Agent> team(Agent... agents) {
        return new ArrayList<>(Arrays.asList(agents));
    }

    // same exploration mission as in scenario 1
    private static ExplorationMission explorationMission() {
        return new ExplorationMission("M-1", "Valles", 3, 40, Skill.GEOLOGY,
                Arrays.asList("Spectrometer"),
                new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5));
    }

    // puts an engineer and a repair robot into a running repair mission, so they are busy
    private static void busyOmarAndFixer(MarsColony colony, String note, String engineerId, String robotId) {
        RepairMission repair = new RepairMission("M-2", colony.findFacility("Lab 1"), 5,
                new ResourceRequirement().with(ResourceType.FUEL, 1));
        SubmissionResult result = colony.submitMission(repair,
                team(colony.findAgent(engineerId), colony.findAgent(robotId)));
        Check.isTrue("setup: repair mission accepted (" + note + ")", result.isAccepted());
    }
}
