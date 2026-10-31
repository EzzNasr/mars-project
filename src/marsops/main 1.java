package marsops;

import marsops.agents.*;
import marsops.colony.*;
import marsops.common.*;
import marsops.emergencies.*;
import marsops.facilities.*;
import marsops.missions.*;
import marsops.simulation.Simulation;

import java.util.List;
import java.util.Set;

/**
 * Main application entry point - Compact Version.
 * @author C
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== MARS-OPS SIMULATION DEMO ===");
        
        boolean allPassed = scenario1_successfulExploration()
                && scenario2_missionRejection()
                && scenario3_resourceShortage()
                && scenario4_emergencyHandling()
                && scenario5_environmentalEffect();

        System.out.println("\nFinal Result: " + (allPassed ? "ALL SCENARIOS PASSED!" : "SOME SCENARIOS FAILED!"));
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
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m1 = new ExplorationMission("M-1", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        
        SubmissionResult res = colony.submitMission(m1, List.of(colony.findAgent("H-01"), colony.findAgent("R-01")));
        sim.advanceTime(3);
        
        boolean pass = res.isAccepted() && m1.getState() == MissionState.COMPLETED;
        System.out.println("Scenario 1 (Successful Exploration): " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    public static boolean scenario2_missionRejection() {
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m2 = new ExplorationMission("M-2", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        
        SubmissionResult res = colony.submitMission(m2, List.of(colony.findAgent("H-03"), colony.findAgent("R-01")));
        
        boolean pass = !res.isAccepted() && m2.getState() == MissionState.FAILED;
        System.out.println("Scenario 2 (Mission Rejection): " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    public static boolean scenario3_resourceShortage() {
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        ResourceRequirement req = new ResourceRequirement().with(ResourceType.OXYGEN, 600);
        ResearchMission m3 = new ResearchMission("M-3", (Laboratory) colony.findFacility("Lab 1"), Skill.CHEMISTRY, 2, List.of("Microscope"), req);
        
        SubmissionResult res = colony.submitMission(m3, List.of(colony.findAgent("H-01")));
        
        boolean pass = !res.isAccepted() && m3.getState() == MissionState.FAILED;
        System.out.println("Scenario 3 (Resource Shortage): " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    public static boolean scenario4_emergencyHandling() {
        Simulation sim1 = buildSampleSimulation();
        Habitat hab1 = (Habitat) sim1.getColony().findFacility("Habitat A");
        OxygenLeak leak1 = new OxygenLeak("E-1", hab1, 80);
        sim1.triggerEmergency(leak1);

        Simulation sim2 = buildSampleSimulation();
        MarsColony col2 = sim2.getColony();
        Habitat hab2 = (Habitat) col2.findFacility("Habitat A");
        col2.submitMission(new RepairMission("M-D", hab2, 5, new ResourceRequirement().with(ResourceType.SPARE_PARTS, 1)), 
                List.of(col2.findAgent("H-02"), col2.findAgent("R-02")));
        OxygenLeak leak2 = new OxygenLeak("E-2", hab2, 80);
        sim2.triggerEmergency(leak2);

        boolean pass = leak1.getState() == EmergencyState.RESOLVED && leak2.getState() == EmergencyState.UNRESOLVED;
        System.out.println("Scenario 4 (Emergency Handling): " + (pass ? "PASS" : "FAIL"));
        return pass;
    }

    public static boolean scenario5_environmentalEffect() {
        Simulation sim1 = buildSampleSimulation();
        sim1.getColony().changeEnvironment(EnvironmentCondition.DUST_STORM);
        ResourceRequirement req = new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5);
        ExplorationMission m1 = new ExplorationMission("M-5A", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        SubmissionResult res1 = sim1.getColony().submitMission(m1, List.of(sim1.getColony().findAgent("H-01"), sim1.getColony().findAgent("R-01")));

        Simulation sim2 = buildSampleSimulation();
        ExplorationMission m2 = new ExplorationMission("M-5B", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"), req);
        sim2.getColony().submitMission(m2, List.of(sim2.getColony().findAgent("H-01"), sim2.getColony().findAgent("R-01")));
        sim2.advanceTime(1);
        sim2.getColony().changeEnvironment(EnvironmentCondition.DUST_STORM);

        boolean pass = !res1.isAccepted() && m2.getState() == MissionState.FAILED;
        System.out.println("Scenario 5 (Environmental Effect): " + (pass ? "PASS" : "FAIL"));
        return pass;
    }
}