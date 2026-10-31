package marsops;

import java.util.List;
import java.util.Set;
import marsops.agents.*;
import marsops.colony.*;
import marsops.common.*;
import marsops.emergencies.*;
import marsops.facilities.*;
import marsops.missions.*;
import marsops.simulation.Simulation;

/**
 * Main application entry point running 5 demonstration scenarios.
 * Features highly formatted, tabular console output for clear demonstration of results.
 * 
 * @author C
 */
public class Main {

    // --- ASCII Formatting Helpers ---
    private static final String LINE = "+-----------------------------------------------------------------------------------------+";
    private static final String THICK_LINE = "===========================================================================================";

    public static void main(String[] args) {
        printHeader("MARS-OPS SIMULATION DEMO - EXECUTIVE SUMMARY");
        
        boolean s1 = scenario1_successfulExploration();
        boolean s2 = scenario2_missionRejection();
        boolean s3 = scenario3_resourceShortage();
        boolean s4 = scenario4_emergencyHandling();
        boolean s5 = scenario5_environmentalEffect();

        boolean allPassed = s1 && s2 && s3 && s4 && s5;

        System.out.println("\n" + THICK_LINE);
        System.out.printf("| %-87s |\n", "FINAL SIMULATION RESULTS");
        System.out.println(THICK_LINE);
        System.out.printf("| %-80s | %-4s |\n", "Scenario 1: Successful Exploration", s1 ? "PASS" : "FAIL");
        System.out.printf("| %-80s | %-4s |\n", "Scenario 2: Mission Rejection", s2 ? "PASS" : "FAIL");
        System.out.printf("| %-80s | %-4s |\n", "Scenario 3: Resource Shortage", s3 ? "PASS" : "FAIL");
        System.out.printf("| %-80s | %-4s |\n", "Scenario 4: Emergency Handling", s4 ? "PASS" : "FAIL");
        System.out.printf("| %-80s | %-4s |\n", "Scenario 5: Environmental Effect", s5 ? "PASS" : "FAIL");
        System.out.println(THICK_LINE);
        System.out.printf("| %-80s | %-4s |\n", "OVERALL SYSTEM STATUS", allPassed ? "PASS" : "FAIL");
        System.out.println(THICK_LINE + "\n");
    }

    private static void printHeader(String title) {
        System.out.println("\n" + THICK_LINE);
        System.out.printf("| %-87s |\n", title);
        System.out.println(THICK_LINE);
    }

    private static void printReportBox(String title, String report) {
        System.out.println(LINE);
        System.out.printf("| %-87s |\n", title);
        System.out.println(LINE);
        String[] lines = report.split("\n");
        for (String l : lines) {
            System.out.printf("| %-87s |\n", l);
        }
        System.out.println(LINE);
    }

    private static void printResult(boolean passed, String details) {
        System.out.printf("| RESULT: %-79s |\n", (passed ? "[PASS]" : "[FAIL]"));
        System.out.printf("| DETAILS: %-78s |\n", details);
        System.out.println(LINE + "\n");
    }

    public static Simulation buildSampleSimulation() {
        MarsColony colony = new MarsColony("Mars Base Alpha", new StorageFirstPolicy());

        Habitat habA = new Habitat("Habitat A", 1000);
        habA.store(ResourceType.OXYGEN, 300);
        habA.store(ResourceType.WATER, 150);
        habA.store(ResourceType.FOOD, 150);

        StorageFacility storage = new StorageFacility("Storage", 2000);
        storage.store(ResourceType.OXYGEN, 200);
        storage.store(ResourceType.WATER, 200);
        storage.store(ResourceType.FOOD, 200);
        storage.store(ResourceType.FUEL, 100);
        storage.store(ResourceType.BATTERY_POWER, 100);
        storage.store(ResourceType.MEDICAL_SUPPLIES, 20);
        storage.store(ResourceType.SPARE_PARTS, 10);

        Laboratory lab1 = new Laboratory("Lab 1", 500);
        lab1.store(ResourceType.OXYGEN, 50);
        lab1.store(ResourceType.BATTERY_POWER, 20);
        lab1.addEquipment(new Equipment("Microscope"));

        PowerStation power1 = new PowerStation("Power 1", 500, 100);
        power1.store(ResourceType.FUEL, 40);
        power1.store(ResourceType.BATTERY_POWER, 100);

        CommunicationStation comm1 = new CommunicationStation("Comm 1", 200);

        colony.addFacility(habA);
        colony.addFacility(storage);
        colony.addFacility(lab1);
        colony.addFacility(power1);
        colony.addFacility(comm1);

        Scientist layla = new Scientist("H-01", "Dr. Layla", habA, Set.of(Skill.GEOLOGY, Skill.CHEMISTRY));
        Engineer omar = new Engineer("H-02", "Eng. Omar", habA, Set.of(Skill.ENGINEERING));
        MedicalOfficer salma = new MedicalOfficer("H-03", "Dr. Salma", habA, Set.of(Skill.MEDICINE));

        ExplorationRover rover1 = new ExplorationRover("R-01", "Rover-1", storage, 100);
        rover1.loadEquipment(new Equipment("Spectrometer"));

        RepairRobot fixer1 = new RepairRobot("R-02", "Fixer-1", storage, 100);
        Drone drone1 = new Drone("R-03", "Drone-1", storage, 100);

        colony.addAgent(layla);
        colony.addAgent(omar);
        colony.addAgent(salma);
        colony.addAgent(rover1);
        colony.addAgent(fixer1);
        colony.addAgent(drone1);

        return new Simulation(colony);
    }

    public static boolean scenario1_successfulExploration() {
        printHeader("SCENARIO 1: SUCCESSFUL EXPLORATION");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        printReportBox("BEFORE MISSION START", sim.report());

        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m1 = new ExplorationMission("M-1", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        
        Agent layla = colony.findAgent("H-01");
        Agent rover1 = colony.findAgent("R-01");
        
        SubmissionResult res = colony.submitMission(m1, List.of(layla, rover1));
        System.out.println("| ACTION: Submitting M-1 (Exploration) with Dr. Layla & Rover-1");
        System.out.printf("| SYSTEM RESPONSE: %-69s |\n", res.toString());
        
        boolean assignedCorrectly = res.isAccepted() && m1.getState() == MissionState.ASSIGNED;
        
        System.out.println("| ACTION: Advancing time by 3 units...");
        sim.advanceTime(3);
        
        printReportBox("AFTER 3 TICKS (COMPLETION)", sim.report());
        
        boolean pass = assignedCorrectly 
            && m1.getState() == MissionState.COMPLETED
            && colony.getResourceManager().getTotal(ResourceType.FUEL) == 130
            && colony.getResourceManager().getTotal(ResourceType.BATTERY_POWER) == 215
            && !layla.isAssigned() && !rover1.isAssigned()
            && layla.getLocation() == layla.getHomeLocation()
            && rover1.getLocation() == rover1.getHomeLocation()
            && ((Robot) rover1).getBattery() == 85
            && ((CrewMember) layla).getFatigue() == 30;
            
        printResult(pass, "Resources consumed, agents returned home, stats updated correctly.");
        return pass;
    }

    public static boolean scenario2_missionRejection() {
        printHeader("SCENARIO 2: MISSION REJECTION (CAPABILITY MISMATCH)");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        printReportBox("INITIAL STATE", sim.report());

        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m2 = new ExplorationMission("M-2", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        
        System.out.println("| ACTION: Submitting M-2 (Exploration) with Dr. Salma (Medical) & Rover-1");
        SubmissionResult res = colony.submitMission(m2, List.of(colony.findAgent("H-03"), colony.findAgent("R-01")));
        
        System.out.printf("| SYSTEM RESPONSE: %-69s |\n", res.toString());
        
        boolean pass = !res.isAccepted() 
            && m2.getState() == MissionState.FAILED
            && colony.getActiveMission() == null
            && colony.getResourceManager().getTotal(ResourceType.FUEL) == 140; // Unchanged
            
        printResult(pass, "Mission rejected due to missing capability. Resources unchanged.");
        return pass;
    }

    public static boolean scenario3_resourceShortage() {
        printHeader("SCENARIO 3: RESOURCE SHORTAGE");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        printReportBox("INITIAL STATE", sim.report());

        ResourceRequirement req = new ResourceRequirement().with(ResourceType.OXYGEN, 600);
        ResearchMission m3 = new ResearchMission("M-3", (Laboratory) colony.findFacility("Lab 1"), Skill.CHEMISTRY, 2, List.of("Microscope"), req);
        
        System.out.println("| ACTION: Submitting M-3 requiring 600 OXYGEN (Only 550 available)");
        SubmissionResult res = colony.submitMission(m3, List.of(colony.findAgent("H-01")));
        
        System.out.printf("| SYSTEM RESPONSE: %-69s |\n", res.toString());
        
        boolean pass = !res.isAccepted() 
            && m3.getState() == MissionState.FAILED
            && res.getReason().contains("Insufficient OXYGEN")
            && colony.getResourceManager().getTotal(ResourceType.OXYGEN) == 550; // Unchanged
            
        printResult(pass, "Mission rejected due to strict resource limits. Totals unchanged.");
        return pass;
    }

    public static boolean scenario4_emergencyHandling() {
        printHeader("SCENARIO 4: EMERGENCY HANDLING (RESOLVED vs UNRESOLVED)");
        
        // 4a - Successful resolution
        Simulation sim1 = buildSampleSimulation();
        MarsColony col1 = sim1.getColony();
        Habitat hab1 = (Habitat) col1.findFacility("Habitat A");
        
        System.out.println("| PHASE 4A: Fully staffed colony encounters an Oxygen Leak.");
        OxygenLeak leak1 = new OxygenLeak("E-1", hab1, 80);
        sim1.triggerEmergency(leak1);
        
        printReportBox("AFTER EMERGENCY 4A (EXPECT RESOLVED)", sim1.report());
        
        boolean pass1 = leak1.getState() == EmergencyState.RESOLVED
            && hab1.getStored(ResourceType.OXYGEN) == 220 // 300 - 80
            && col1.getResourceManager().getTotal(ResourceType.OXYGEN) == 470
            && col1.getResourceManager().getTotal(ResourceType.SPARE_PARTS) == 8
            && col1.getResourceManager().getTotal(ResourceType.BATTERY_POWER) == 215
            && !col1.findAgent("H-02").isAssigned()
            && !col1.findAgent("R-02").isAssigned()
            && col1.findAgent("H-02").getLocation() == hab1
            && !hab1.isLeaking();

        // 4b - Unresolved due to unavailable responders
        Simulation sim2 = buildSampleSimulation();
        MarsColony col2 = sim2.getColony();
        Habitat hab2 = (Habitat) col2.findFacility("Habitat A");
        
        System.out.println("| PHASE 4B: Responders are heavily damaged prior to leak.");
        col2.findAgent("H-02").takeDamage(90); 
        col2.findAgent("R-02").takeDamage(90);
        
        OxygenLeak leak2 = new OxygenLeak("E-2", hab2, 80);
        sim2.triggerEmergency(leak2);
        
        printReportBox("AFTER EMERGENCY 4B (EXPECT UNRESOLVED)", sim2.report());
        
        boolean pass2 = leak2.getState() == EmergencyState.UNRESOLVED
            && leak2.getReason() != null && !leak2.getReason().isEmpty()
            && hab2.getStored(ResourceType.OXYGEN) == 140 // 300 - 80 (initial) - 80 (penalty)
            && col2.getResourceManager().getTotal(ResourceType.SPARE_PARTS) == 10 // No resources consumed
            && col2.getResourceManager().getTotal(ResourceType.BATTERY_POWER) == 220;

        boolean pass = pass1 && pass2;
        printResult(pass, "Both resolved (single loss, items used) and unresolved (double loss) verified.");
        return pass;
    }

    public static boolean scenario5_environmentalEffect() {
        printHeader("SCENARIO 5: ENVIRONMENTAL RESTRICTIONS");
        
        // 5a - Start Blocked
        Simulation sim1 = buildSampleSimulation();
        System.out.println("| PHASE 5A: Submitting Rover mission DURING a Dust Storm.");
        sim1.getColony().changeEnvironment(EnvironmentCondition.DUST_STORM);
        
        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m1 = new ExplorationMission("M-5A", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        SubmissionResult res1 = sim1.getColony().submitMission(m1, List.of(sim1.getColony().findAgent("H-01"), sim1.getColony().findAgent("R-01")));
        
        System.out.printf("| SYSTEM RESPONSE: %-69s |\n", res1.toString());

        // 5b - Aborted mid-mission
        Simulation sim2 = buildSampleSimulation();
        System.out.println("| PHASE 5B: Dust storm hits DURING an active Rover mission.");
        ExplorationMission m2 = new ExplorationMission("M-5B", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        sim2.getColony().submitMission(m2, List.of(sim2.getColony().findAgent("H-01"), sim2.getColony().findAgent("R-01")));
        
        sim2.advanceTime(1);
        System.out.println("| ACTION: Dust storm initiated at t=1");
        sim2.getColony().changeEnvironment(EnvironmentCondition.DUST_STORM);
        
        printReportBox("AFTER DUST STORM (EXPECT ABORTED)", sim2.report());

        boolean pass = !res1.isAccepted() 
            && m2.getState() == MissionState.FAILED
            && m2.getFailureReason().contains("Dust storm")
            && !sim2.getColony().findAgent("H-01").isAssigned()
            && !sim2.getColony().findAgent("R-01").isAssigned();
            
        printResult(pass, "Start blocked correctly; Active mission aborted and agents released.");
        return pass;
    }
}