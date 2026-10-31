package marsops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import marsops.agents.*;
import marsops.colony.*; // B's classes
import marsops.common.*;
import marsops.emergencies.OxygenLeak;
import marsops.facilities.*;
import marsops.missions.*; // B's classes
import marsops.simulation.Simulation;

// Runs the 5 demo scenarios. Every scenario returns true only if all its checks passed.
public class Main {

    public static void main(String[] args) {
        boolean[] results = {
            scenario1_successfulExploration(), scenario2_missionRejection(), scenario3_resourceShortage(),
            scenario4_emergencyHandling(), scenario5_environmentalEffect()
        };
        boolean all = true;
        for (int i = 0; i < results.length; i++) {
            if (!results[i]) {
                all = false;
                System.out.println("FAILED: scenario " + (i + 1));
            }
        }
        System.out.println(all ? "\nALL SCENARIOS PASSED" : "\nSOME SCENARIOS FAILED");
    }

    // the sample colony from section 11 (the order of the facilities matters)
    public static Simulation buildSampleSimulation() {
        MarsColony colony = new MarsColony("Mars Base Alpha");

        Habitat habitatA = new Habitat("Habitat A", 1000);
        fill(habitatA, ResourceType.OXYGEN, 300, ResourceType.WATER, 150, ResourceType.FOOD, 150);
        StorageFacility storage = new StorageFacility("Storage", 2000);
        fill(storage, ResourceType.OXYGEN, 200, ResourceType.WATER, 200, ResourceType.FOOD, 200,
                ResourceType.FUEL, 100, ResourceType.BATTERY_POWER, 100,
                ResourceType.MEDICAL_SUPPLIES, 20, ResourceType.SPARE_PARTS, 10);
        Laboratory lab = new Laboratory("Lab 1", 500);
        fill(lab, ResourceType.OXYGEN, 50, ResourceType.BATTERY_POWER, 20);
        lab.addEquipment(new Equipment("Microscope"));
        PowerStation power = new PowerStation("Power 1", 500, 100);
        fill(power, ResourceType.FUEL, 40, ResourceType.BATTERY_POWER, 100);

        for (Facility f : List.of(habitatA, storage, lab, power, new CommunicationStation("Comm 1", 200))) {
            colony.addFacility(f);
        }

        ExplorationRover rover = new ExplorationRover("R-01", "Rover-1", storage, 100);
        rover.loadEquipment(new Equipment("Spectrometer"));
        colony.addAgent(new Scientist("H-01", "Dr. Layla", habitatA, Set.of(Skill.GEOLOGY, Skill.CHEMISTRY)));
        colony.addAgent(new Engineer("H-02", "Eng. Omar", habitatA, Set.of(Skill.ENGINEERING)));
        colony.addAgent(new MedicalOfficer("H-03", "Dr. Salma", habitatA, Set.of(Skill.MEDICINE)));
        colony.addAgent(rover);
        colony.addAgent(new RepairRobot("R-02", "Fixer-1", storage, 100));
        colony.addAgent(new Drone("R-03", "Drone-1", storage, 100));
        return new Simulation(colony);
    }

    // a normal exploration mission with Dr. Layla and the rover
    public static boolean scenario1_successfulExploration() {
        heading("SCENARIO 1 - Successful exploration");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Agent layla = colony.findAgent("H-01");
        Agent rover = colony.findAgent("R-01");
        int fuel = total(colony, ResourceType.FUEL);
        int battery = total(colony, ResourceType.BATTERY_POWER);
        System.out.println(sim.report());

        ExplorationMission mission = explorationMission();
        SubmissionResult result = colony.submitMission(mission, team(layla, rover));
        System.out.println(result);
        boolean ok = check("accepted and ASSIGNED", result.isAccepted() && mission.getState() == MissionState.ASSIGNED);

        sim.advanceTime(3);
        System.out.println(sim.report());
        ok &= check("COMPLETED after 3 ticks", mission.getState() == MissionState.COMPLETED);
        ok &= check("FUEL -10 and BATTERY_POWER -5",
                total(colony, ResourceType.FUEL) == fuel - 10 && total(colony, ResourceType.BATTERY_POWER) == battery - 5);
        ok &= check("agents released and back home",
                !layla.isAssigned() && !rover.isAssigned()
                && layla.getLocation() == layla.getHomeLocation() && rover.getLocation() == rover.getHomeLocation());
        ok &= check("Rover-1 battery is 85 and Dr. Layla fatigue is 30",
                ((ExplorationRover) rover).getBattery() == 85 && ((CrewMember) layla).getFatigue() == 30);
        return ok;
    }

    // the medical officer can't do an exploration mission
    public static boolean scenario2_missionRejection() {
        heading("SCENARIO 2 - Mission rejection");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        var totalsBefore = colony.getResourceManager().getAllTotals();
        System.out.println(sim.report());

        ExplorationMission mission = explorationMission();
        SubmissionResult result = colony.submitMission(mission,
                team(colony.findAgent("H-03"), colony.findAgent("R-01")));
        System.out.println("Mission cannot be assigned.");
        System.out.println("Reason: " + result.getReason());
        System.out.println(sim.report());

        boolean ok = check("rejected because of the missing capability",
                !result.isAccepted() && result.getReason().contains("Dr. Salma does not possess the required capability"));
        ok &= check("mission FAILED and no active mission",
                mission.getState() == MissionState.FAILED && colony.getActiveMission() == null);
        ok &= check("resources unchanged", totalsBefore.equals(colony.getResourceManager().getAllTotals()));
        return ok;
    }

    // the research mission needs 600 oxygen but the colony only has 550
    public static boolean scenario3_resourceShortage() {
        heading("SCENARIO 3 - Resource shortage");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        var totalsBefore = colony.getResourceManager().getAllTotals();
        System.out.println(sim.report());

        ResearchMission mission = new ResearchMission("M-3", (Laboratory) colony.findFacility("Lab 1"),
                Skill.CHEMISTRY, 2, List.of("Microscope"), new ResourceRequirement().with(ResourceType.OXYGEN, 600));
        SubmissionResult result = colony.submitMission(mission, team(colony.findAgent("H-01")));
        System.out.println(result);
        System.out.println(sim.report());

        var totalsAfter = colony.getResourceManager().getAllTotals();
        boolean ok = check("rejected: Insufficient OXYGEN",
                !result.isAccepted() && result.getReason().contains("Insufficient OXYGEN"));
        ok &= check("every total unchanged and not negative",
                totalsBefore.equals(totalsAfter) && totalsAfter.values().stream().allMatch(v -> v >= 0));
        return ok;
    }

    // 4a: the leak is repaired. 4b: Omar and Fixer-1 are busy, so nobody can repair it
    public static boolean scenario4_emergencyHandling() {
        heading("SCENARIO 4 - Emergency handling");
        System.out.println("--- 4a: leak repaired ---");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        Habitat habitat = (Habitat) colony.findFacility("Habitat A");
        Agent omar = colony.findAgent("H-02");
        Agent fixer = colony.findAgent("R-02");
        int oxygen = total(colony, ResourceType.OXYGEN);
        int spares = total(colony, ResourceType.SPARE_PARTS);
        int battery = total(colony, ResourceType.BATTERY_POWER);
        System.out.println(sim.report());

        OxygenLeak leak = new OxygenLeak("E-1", habitat, 80);
        sim.triggerEmergency(leak);
        System.out.println(sim.report());

        boolean ok = check("4a: RESOLVED, Habitat A oxygen 300 -> 220, colony oxygen -80",
                leak.getState() == EmergencyState.RESOLVED && habitat.getStored(ResourceType.OXYGEN) == 220
                && total(colony, ResourceType.OXYGEN) == oxygen - 80);
        ok &= check("4a: SPARE_PARTS -2 and BATTERY_POWER -5",
                total(colony, ResourceType.SPARE_PARTS) == spares - 2 && total(colony, ResourceType.BATTERY_POWER) == battery - 5);
        ok &= check("4a: responders at Habitat A and released, habitat not leaking",
                omar.getLocation() == habitat && fixer.getLocation() == habitat
                && !omar.isAssigned() && !fixer.isAssigned() && !habitat.isLeaking());

        System.out.println("--- 4b: leak unresolved ---");
        Simulation sim2 = buildSampleSimulation();
        MarsColony colony2 = sim2.getColony();
        Habitat habitat2 = (Habitat) colony2.findFacility("Habitat A");
        // a repair mission keeps both responders busy
        colony2.submitMission(new RepairMission("M-2", colony2.findFacility("Lab 1"), 5,
                new ResourceRequirement().with(ResourceType.FUEL, 1)), team(colony2.findAgent("H-02"), colony2.findAgent("R-02")));
        int spares2 = total(colony2, ResourceType.SPARE_PARTS);
        int battery2 = total(colony2, ResourceType.BATTERY_POWER);

        OxygenLeak leak2 = new OxygenLeak("E-1", habitat2, 80);
        sim2.triggerEmergency(leak2);
        System.out.println(sim2.report());

        ok &= check("4b: UNRESOLVED with a reason, Habitat A oxygen 300 -> 140",
                leak2.getState() == EmergencyState.UNRESOLVED && !leak2.getReason().isEmpty()
                && habitat2.getStored(ResourceType.OXYGEN) == 140);
        ok &= check("4b: no spare parts or battery used",
                total(colony2, ResourceType.SPARE_PARTS) == spares2 && total(colony2, ResourceType.BATTERY_POWER) == battery2);
        return ok;
    }

    // 5a: a dust storm blocks a new mission. 5b: a dust storm stops a running mission
    public static boolean scenario5_environmentalEffect() {
        heading("SCENARIO 5 - Environmental effect");
        System.out.println("--- 5a: start blocked ---");
        Simulation sim = buildSampleSimulation();
        MarsColony colony = sim.getColony();
        colony.changeEnvironment(EnvironmentCondition.DUST_STORM);
        SubmissionResult result = colony.submitMission(explorationMission(),
                team(colony.findAgent("H-01"), colony.findAgent("R-01")));
        System.out.println("Exploration Mission cannot start.");
        System.out.println("Reason: " + result.getReason());
        boolean ok = check("5a: rejected because of the dust storm",
                !result.isAccepted() && result.getReason().contains("Surface operations are not allowed during a dust storm"));

        System.out.println("--- 5b: running mission aborted ---");
        Simulation sim2 = buildSampleSimulation();
        MarsColony colony2 = sim2.getColony();
        Agent layla = colony2.findAgent("H-01");
        Agent rover = colony2.findAgent("R-01");
        ExplorationMission mission = explorationMission();
        colony2.submitMission(mission, team(layla, rover));
        sim2.advanceTime(1);
        colony2.changeEnvironment(EnvironmentCondition.DUST_STORM);
        System.out.println(sim2.report());

        ok &= check("5b: mission FAILED because of the dust storm, agents released",
                mission.getState() == MissionState.FAILED && mission.getFailureReason().contains("Dust storm")
                && !layla.isAssigned() && !rover.isAssigned());
        return ok;
    }

    // ---------- small helpers ----------

    // a new mission every time because a mission can only be used once
    private static ExplorationMission explorationMission() {
        return new ExplorationMission("M-1", "Valles", 3, 40, Skill.GEOLOGY, List.of("Spectrometer"),
                new ResourceRequirement().with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 5));
    }

    // fill(facility, TYPE, amount, TYPE, amount, ...)
    private static void fill(Facility facility, Object... typesAndAmounts) {
        for (int i = 0; i < typesAndAmounts.length; i += 2) {
            facility.store((ResourceType) typesAndAmounts[i], (Integer) typesAndAmounts[i + 1]);
        }
    }

    private static List<Agent> team(Agent... agents) {
        return new ArrayList<>(Arrays.asList(agents));
    }

    private static int total(MarsColony colony, ResourceType type) {
        return colony.getResourceManager().getTotal(type);
    }

    private static boolean check(String label, boolean condition) {
        System.out.println("  [" + (condition ? "OK" : "FAIL") + "] " + label);
        return condition;
    }

    private static void heading(String text) {
        System.out.println("\n==================================================\n" + text
                + "\n==================================================");
    }
}
