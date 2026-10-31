package marsops.tests;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import marsops.agents.Agent;
import marsops.agents.Drone;
import marsops.agents.Engineer;
import marsops.agents.ExplorationRover;
import marsops.agents.MedicalOfficer;
import marsops.agents.RepairRobot;
import marsops.agents.Scientist;
import marsops.colony.MarsColony;
import marsops.colony.SubmissionResult;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.Equipment;
import marsops.common.FacilityStatus;
import marsops.common.MartianSurface;
import marsops.common.MissionState;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.common.Skill;
import marsops.facilities.CommunicationStation;
import marsops.facilities.Facility;
import marsops.facilities.Habitat;
import marsops.facilities.Laboratory;
import marsops.facilities.PowerStation;
import marsops.facilities.StorageFacility;
import marsops.missions.ExplorationMission;
import marsops.missions.Mission;
import marsops.missions.RepairMission;
import marsops.missions.RescueMission;
import marsops.missions.ResearchMission;

/**
 * Tests for Member B's part: missions, resource manager and the colony rules.
 * Run with: java -cp out marsops.tests.ColonyMissionsTests
 *
 * @author B
 */
public final class ColonyMissionsTests {

    private ColonyMissionsTests() { }

    /** The sample colony of playbook Section 11, rebuilt fresh for every test. */
    private static final class Fixture {
        final MarsColony colony = new MarsColony("Mars Base Alpha");
        final Habitat habitatA = new Habitat("Habitat A", 1000);
        final StorageFacility storage = new StorageFacility("Storage", 2000);
        final Laboratory lab = new Laboratory("Lab 1", 500);
        final PowerStation power = new PowerStation("Power 1", 500, 100);
        final CommunicationStation comm = new CommunicationStation("Comm 1", 200);
        final Scientist layla;
        final Engineer omar;
        final MedicalOfficer salma;
        final ExplorationRover rover;
        final RepairRobot fixer;
        final Drone drone;

        Fixture() {
            this(100);
        }

        Fixture(int roverBattery) {
            habitatA.store(ResourceType.OXYGEN, 300);
            habitatA.store(ResourceType.WATER, 150);
            habitatA.store(ResourceType.FOOD, 150);
            storage.store(ResourceType.OXYGEN, 200);
            storage.store(ResourceType.WATER, 200);
            storage.store(ResourceType.FOOD, 200);
            storage.store(ResourceType.FUEL, 100);
            storage.store(ResourceType.BATTERY_POWER, 100);
            storage.store(ResourceType.MEDICAL_SUPPLIES, 20);
            storage.store(ResourceType.SPARE_PARTS, 10);
            lab.store(ResourceType.OXYGEN, 50);
            lab.store(ResourceType.BATTERY_POWER, 20);
            lab.addEquipment(new Equipment("Microscope"));
            power.store(ResourceType.FUEL, 40);
            power.store(ResourceType.BATTERY_POWER, 100);
            colony.addFacility(habitatA);
            colony.addFacility(storage);
            colony.addFacility(lab);
            colony.addFacility(power);
            colony.addFacility(comm);

            layla = new Scientist("H-01", "Dr. Layla", habitatA,
                    EnumSet.of(Skill.GEOLOGY, Skill.CHEMISTRY));
            omar = new Engineer("H-02", "Eng. Omar", habitatA, EnumSet.of(Skill.ENGINEERING));
            salma = new MedicalOfficer("H-03", "Dr. Salma", habitatA, EnumSet.of(Skill.MEDICINE));
            rover = new ExplorationRover("R-01", "Rover-1", storage, roverBattery);
            rover.loadEquipment(new Equipment("Spectrometer"));
            fixer = new RepairRobot("R-02", "Fixer-1", storage);
            drone = new Drone("R-03", "Drone-1", storage);
            colony.addAgent(layla);
            colony.addAgent(omar);
            colony.addAgent(salma);
            colony.addAgent(rover);
            colony.addAgent(fixer);
            colony.addAgent(drone);
        }

        ResourceRequirement basicCost() {
            return new ResourceRequirement()
                    .with(ResourceType.FUEL, 10).with(ResourceType.BATTERY_POWER, 20);
        }

        ExplorationMission exploration(String id) {
            return exploration(id, 3, 40);
        }

        ExplorationMission exploration(String id, int duration, int minBattery) {
            return new ExplorationMission(id, "Olympus Region", duration, minBattery,
                    Skill.GEOLOGY, List.of("Spectrometer"), basicCost());
        }

        List<Agent> explorationTeam() {
            return List.<Agent>of(layla, rover);
        }

        ResearchMission research(String id) {
            return new ResearchMission(id, lab, null, 3, List.of("Microscope"),
                    new ResourceRequirement());
        }

        int total(ResourceType type) {
            return colony.getResourceManager().getTotal(type);
        }
    }

    // ------------------------------------------------------------ helpers

    private static void runToCompletion(Mission mission, List<Agent> team) {
        mission.assign(team);
        mission.start();
        for (int i = 0; i < mission.getDurationUnits(); i++) {
            mission.advance();
        }
    }

    private static Map<String, Map<ResourceType, Integer>> snapshotAll(MarsColony colony) {
        Map<String, Map<ResourceType, Integer>> result = new LinkedHashMap<>();
        for (Facility facility : colony.getFacilities()) {
            Map<ResourceType, Integer> stock = new EnumMap<>(ResourceType.class);
            for (ResourceType type : ResourceType.values()) {
                stock.put(type, facility.getStored(type));
            }
            result.put(facility.getName(), stock);
        }
        return result;
    }

    private static boolean noNegativeTotals(Fixture f) {
        for (ResourceType type : ResourceType.values()) {
            if (f.total(type) < 0) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------- mission lifecycle

    private static void testB01() {
        Check.section("B-01 happy path");
        Fixture f = new Fixture();
        ExplorationMission m = f.exploration("M-1");
        Check.equal("B-01 starts CREATED", MissionState.CREATED, m.getState());
        m.assign(f.explorationTeam());
        Check.equal("B-01 ASSIGNED", MissionState.ASSIGNED, m.getState());
        m.start();
        Check.equal("B-01 IN_PROGRESS", MissionState.IN_PROGRESS, m.getState());
        for (int i = 0; i < m.getDurationUnits(); i++) {
            m.advance();
        }
        Check.equal("B-01 COMPLETED", MissionState.COMPLETED, m.getState());
        Check.equal("B-01 progress equals duration", m.getDurationUnits(), m.getProgressUnits());
        Check.isTrue("B-01 isFinished", m.isFinished());
    }

    private static void testB02() {
        Check.section("B-02 invalid transitions");
        Fixture f = new Fixture();
        ExplorationMission created = f.exploration("M-1");
        Check.throwsException("B-02 start on CREATED", IllegalStateException.class, created::start);
        ExplorationMission assigned = f.exploration("M-2");
        assigned.assign(f.explorationTeam());
        Check.throwsException("B-02 advance on ASSIGNED", IllegalStateException.class,
                assigned::advance);
        Check.throwsException("B-02 assign twice", IllegalStateException.class,
                () -> assigned.assign(f.explorationTeam()));
        ExplorationMission done = f.exploration("M-3");
        runToCompletion(done, f.explorationTeam());
        Check.throwsException("B-02 fail on COMPLETED", IllegalStateException.class,
                () -> done.fail("late"));
    }

    private static void testB03() {
        Check.section("B-03 fail rules");
        Fixture f = new Fixture();
        ExplorationMission fromCreated = f.exploration("M-1");
        fromCreated.fail("x");
        Check.equal("B-03 fail from CREATED", MissionState.FAILED, fromCreated.getState());
        Check.equal("B-03 reason kept", "x", fromCreated.getFailureReason());
        ExplorationMission fromAssigned = f.exploration("M-2");
        fromAssigned.assign(f.explorationTeam());
        fromAssigned.fail("x");
        Check.equal("B-03 fail from ASSIGNED", MissionState.FAILED, fromAssigned.getState());
        ExplorationMission fromRunning = f.exploration("M-3");
        fromRunning.assign(f.explorationTeam());
        fromRunning.start();
        fromRunning.fail("x");
        Check.equal("B-03 fail from IN_PROGRESS", MissionState.FAILED, fromRunning.getState());
        ExplorationMission fresh = f.exploration("M-4");
        Check.throwsException("B-03 blank reason", IllegalArgumentException.class,
                () -> fresh.fail(""));
        Check.throwsException("B-03 null reason", IllegalArgumentException.class,
                () -> fresh.fail(null));
        Check.throwsException("B-03 duration 0", IllegalArgumentException.class,
                () -> new ExplorationMission("M-5", "R", 0, 0, null, null, null));
    }

    private static void testB04() {
        Check.section("B-04 requirement getters");
        Fixture f = new Fixture();
        ExplorationMission exp = f.exploration("M-1");
        RepairMission rep = new RepairMission("M-2", f.lab, 2, new ResourceRequirement());
        ResearchMission res = f.research("M-3");
        RescueMission resc = new RescueMission("M-4", "Technician", MartianSurface.INSTANCE,
                2, 30, new ResourceRequirement());
        Check.equal("B-04 exploration caps",
                EnumSet.of(Capability.RESEARCH, Capability.SURFACE_TRAVEL),
                exp.getRequiredCapabilities());
        Check.equal("B-04 repair caps",
                EnumSet.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR),
                rep.getRequiredCapabilities());
        Check.equal("B-04 research caps", EnumSet.of(Capability.RESEARCH),
                res.getRequiredCapabilities());
        Check.equal("B-04 rescue caps", EnumSet.of(Capability.VEHICLE, Capability.MEDICAL),
                resc.getRequiredCapabilities());
        Check.isTrue("B-04 only exploration requires surface",
                exp.requiresSurface() && !rep.requiresSurface()
                        && !res.requiresSurface() && !resc.requiresSurface());
        Check.isTrue("B-04 only rescue requires communication",
                resc.requiresCommunication() && !exp.requiresCommunication()
                        && !rep.requiresCommunication() && !res.requiresCommunication());
        Check.equal("B-04 exploration targets", 0, exp.getTargetFacilities().size());
        Check.equal("B-04 repair targets", 1, rep.getTargetFacilities().size());
        Check.equal("B-04 research targets", 1, res.getTargetFacilities().size());
    }

    private static void testB05() {
        Check.section("B-05 start block reasons");
        Fixture f = new Fixture();
        ExplorationMission exp = f.exploration("M-1");
        Check.contains("B-05 exploration + dust storm",
                exp.getStartBlockReason(EnvironmentCondition.DUST_STORM), "dust storm");
        Check.isTrue("B-05 exploration + normal is null",
                exp.getStartBlockReason(EnvironmentCondition.NORMAL) == null);
        RescueMission rescue = new RescueMission("M-2", "Technician", MartianSurface.INSTANCE,
                2, 30, new ResourceRequirement());
        Check.isTrue("B-05 rescue + comm disruption",
                rescue.getStartBlockReason(EnvironmentCondition.COMMUNICATION_DISRUPTION) != null);
        ResearchMission research = f.research("M-3");
        f.lab.setStatus(FacilityStatus.UNAVAILABLE);
        Check.isTrue("B-05 research with unavailable lab",
                research.getStartBlockReason(EnvironmentCondition.NORMAL) != null);
        f.lab.setStatus(FacilityStatus.OPERATIONAL);
        ResearchMission needsTelescope = new ResearchMission("M-4", f.lab, null, 2,
                List.of("Telescope"), new ResourceRequirement());
        Check.isTrue("B-05 research with missing equipment",
                needsTelescope.getStartBlockReason(EnvironmentCondition.NORMAL) != null);
        f.power.setStatus(FacilityStatus.DESTROYED);
        RepairMission repair = new RepairMission("M-5", f.power, 2, new ResourceRequirement());
        Check.isTrue("B-05 repair of destroyed facility",
                repair.getStartBlockReason(EnvironmentCondition.NORMAL) != null);
    }

    private static void testB06() {
        Check.section("B-06 in-progress failure reasons");
        Fixture f = new Fixture();
        Check.isTrue("B-06 exploration + dust storm", f.exploration("M-1")
                .getInProgressFailureReason(EnvironmentCondition.DUST_STORM) != null);
        Check.isTrue("B-06 research + dust storm is null", f.research("M-2")
                .getInProgressFailureReason(EnvironmentCondition.DUST_STORM) == null);
        RescueMission rescue = new RescueMission("M-3", "Technician", MartianSurface.INSTANCE,
                2, 30, new ResourceRequirement());
        Check.isTrue("B-06 rescue + comm disruption", rescue
                .getInProgressFailureReason(EnvironmentCondition.COMMUNICATION_DISRUPTION) != null);
    }

    private static void testB07() {
        Check.section("B-07 repair completion");
        Fixture f = new Fixture();
        f.lab.setStatus(FacilityStatus.DAMAGED);
        RepairMission repair = new RepairMission("M-1", f.lab, 2, new ResourceRequirement());
        runToCompletion(repair, List.<Agent>of(f.omar, f.fixer));
        Check.equal("B-07 target operational again", FacilityStatus.OPERATIONAL,
                f.lab.getStatus());
    }

    // ------------------------------------------------- resource manager

    private static void testB08() {
        Check.section("B-08 totals count only operational facilities");
        Fixture f = new Fixture();
        Check.equal("B-08 full oxygen total", 550, f.total(ResourceType.OXYGEN));
        f.lab.setStatus(FacilityStatus.DESTROYED);
        f.storage.setStatus(FacilityStatus.UNAVAILABLE);
        Check.equal("B-08 oxygen after losing two facilities", 300,
                f.total(ResourceType.OXYGEN));
    }

    private static void testB09() {
        Check.section("B-09 canSupply and explainShortage");
        Fixture f = new Fixture();
        f.lab.withdraw(ResourceType.OXYGEN, 50);
        ResourceRequirement ok = new ResourceRequirement().with(ResourceType.OXYGEN, 450);
        ResourceRequirement tooBig = new ResourceRequirement().with(ResourceType.OXYGEN, 600);
        Check.isTrue("B-09 450 can be supplied", f.colony.getResourceManager().canSupply(ok));
        Check.isTrue("B-09 600 cannot be supplied",
                !f.colony.getResourceManager().canSupply(tooBig));
        String text = f.colony.getResourceManager().explainShortage(tooBig);
        Check.contains("B-09 names the resource", text, "OXYGEN");
        Check.contains("B-09 states need", text, "need 600");
        Check.contains("B-09 states available", text, "available 500");
    }

    private static void testB10() {
        Check.section("B-10 all-or-nothing");
        Fixture f = new Fixture();
        Map<String, Map<ResourceType, Integer>> before = snapshotAll(f.colony);
        boolean consumed = f.colony.getResourceManager()
                .consume(new ResourceRequirement().with(ResourceType.OXYGEN, 600));
        Check.isTrue("B-10 consume returns false", !consumed);
        Check.equal("B-10 every facility unchanged", before, snapshotAll(f.colony));
    }

    private static void testB11() {
        Check.section("B-11 policy order");
        Fixture f = new Fixture();
        boolean consumed = f.colony.getResourceManager()
                .consume(new ResourceRequirement().with(ResourceType.OXYGEN, 450));
        Check.isTrue("B-11 consume returns true", consumed);
        Check.equal("B-11 Storage emptied first", 0, f.storage.getStored(ResourceType.OXYGEN));
        Check.equal("B-11 Habitat A second", 50, f.habitatA.getStored(ResourceType.OXYGEN));
        Check.equal("B-11 Lab 1 last", 50, f.lab.getStored(ResourceType.OXYGEN));
    }

    private static void testB12() {
        Check.section("B-12 second resource short");
        Fixture f = new Fixture();
        ResourceRequirement req = new ResourceRequirement()
                .with(ResourceType.OXYGEN, 100).with(ResourceType.MEDICAL_SUPPLIES, 50);
        boolean consumed = f.colony.getResourceManager().consume(req);
        Check.isTrue("B-12 consume returns false", !consumed);
        Check.equal("B-12 oxygen not consumed", 550, f.total(ResourceType.OXYGEN));
        Check.equal("B-12 supplies untouched", 20, f.total(ResourceType.MEDICAL_SUPPLIES));
        Check.isTrue("B-12 totals never negative", noNegativeTotals(f));
    }

    // ------------------------------------------------------ colony rules

    private static void testB13() {
        Check.section("B-13 second mission while one is active");
        Fixture f = new Fixture();
        f.colony.submitMission(f.exploration("M-1"), f.explorationTeam());
        ResearchMission second = f.research("M-2");
        SubmissionResult result = f.colony.submitMission(second, List.<Agent>of(f.layla));
        Check.isTrue("B-13 rejected", !result.isAccepted());
        Check.contains("B-13 reason", result.getReason(), "already active");
        Check.equal("B-13 rejected mission FAILED", MissionState.FAILED, second.getState());
        Check.equal("B-13 first mission still active", "M-1", f.colony.getActiveMission().getId());
        Check.equal("B-13 history size", 2, f.colony.getMissionHistory().size());
    }

    private static void testB14() {
        Check.section("B-14 unsuitable agent");
        Fixture f = new Fixture();
        SubmissionResult result = f.colony.submitMission(f.exploration("M-1"),
                List.<Agent>of(f.salma));
        Check.isTrue("B-14 rejected", !result.isAccepted());
        Check.contains("B-14 reason", result.getReason(), "required capability");
        Check.equal("B-14 resources unchanged", 140, f.total(ResourceType.FUEL));
        Check.isTrue("B-14 no active mission", f.colony.getActiveMission() == null);
    }

    private static void testB15() {
        Check.section("B-15 resource shortage");
        Fixture f = new Fixture();
        ExplorationMission big = new ExplorationMission("M-1", "Olympus Region", 3, 40,
                Skill.GEOLOGY, List.of("Spectrometer"),
                new ResourceRequirement().with(ResourceType.OXYGEN, 600));
        SubmissionResult result = f.colony.submitMission(big, f.explorationTeam());
        Check.isTrue("B-15 rejected", !result.isAccepted());
        Check.contains("B-15 reason", result.getReason(), "Insufficient OXYGEN");
        Check.equal("B-15 oxygen unchanged", 550, f.total(ResourceType.OXYGEN));
        Check.isTrue("B-15 totals never negative", noNegativeTotals(f));
    }

    private static void testB16() {
        Check.section("B-16 accepted mission");
        Fixture f = new Fixture();
        ExplorationMission m = f.exploration("M-1");
        SubmissionResult result = f.colony.submitMission(m, f.explorationTeam());
        Check.isTrue("B-16 accepted", result.isAccepted());
        Check.equal("B-16 state ASSIGNED", MissionState.ASSIGNED, m.getState());
        Check.isTrue("B-16 is the active mission", f.colony.getActiveMission() == m);
        Check.isTrue("B-16 agents assigned", f.layla.isAssigned() && f.rover.isAssigned());
        Check.equal("B-16 fuel consumed once", 130, f.total(ResourceType.FUEL));
        Check.equal("B-16 battery consumed once", 200, f.total(ResourceType.BATTERY_POWER));
    }

    private static void testB17() {
        Check.section("B-17 ticks");
        Fixture f = new Fixture();
        ExplorationMission m = f.exploration("M-1");
        f.colony.submitMission(m, f.explorationTeam());
        f.colony.tick();
        Check.equal("B-17 IN_PROGRESS after 1 tick", MissionState.IN_PROGRESS, m.getState());
        Check.equal("B-17 progress 1", 1, m.getProgressUnits());
        Check.isTrue("B-17 rover on the surface", f.rover.getLocation() == MartianSurface.INSTANCE);
        f.colony.tick();
        f.colony.tick();
        Check.equal("B-17 COMPLETED after 3 ticks", MissionState.COMPLETED, m.getState());
        Check.isTrue("B-17 agents released", !f.layla.isAssigned() && !f.rover.isAssigned());
        Check.isTrue("B-17 no active mission", f.colony.getActiveMission() == null);
        Check.isTrue("B-17 rover back home", f.rover.getLocation() == f.storage);
    }

    private static void testB18() {
        Check.section("B-18 failed missions are not retried");
        Fixture f = new Fixture();
        ExplorationMission m = f.exploration("M-1");
        f.colony.changeEnvironment(EnvironmentCondition.DUST_STORM);
        f.colony.submitMission(m, f.explorationTeam());
        f.colony.changeEnvironment(EnvironmentCondition.NORMAL);
        SubmissionResult again = f.colony.submitMission(m, f.explorationTeam());
        Check.isTrue("B-18 rejected again", !again.isAccepted());
        Check.contains("B-18 reason", again.getReason(), "already processed");
    }

    private static void testB19() {
        Check.section("B-19 dust storm");
        Fixture blocked = new Fixture();
        blocked.colony.changeEnvironment(EnvironmentCondition.DUST_STORM);
        SubmissionResult result = blocked.colony.submitMission(blocked.exploration("M-1"),
                blocked.explorationTeam());
        Check.isTrue("B-19 rejected during storm", !result.isAccepted());
        Check.contains("B-19 reason", result.getReason(), "dust storm");

        Fixture running = new Fixture();
        ExplorationMission m = running.exploration("M-2");
        running.colony.submitMission(m, running.explorationTeam());
        running.colony.tick();
        running.colony.changeEnvironment(EnvironmentCondition.DUST_STORM);
        Check.equal("B-19 running exploration FAILED", MissionState.FAILED, m.getState());
        Check.contains("B-19 failure reason", m.getFailureReason(), "dust storm");
        Check.isTrue("B-19 agents released",
                !running.layla.isAssigned() && !running.rover.isAssigned());

        Fixture other = new Fixture();
        ResearchMission research = other.research("M-3");
        other.colony.submitMission(research, List.<Agent>of(other.layla));
        other.colony.tick();
        other.colony.changeEnvironment(EnvironmentCondition.DUST_STORM);
        Check.equal("B-19 research not affected", MissionState.IN_PROGRESS, research.getState());
    }

    private static void testB20() {
        Check.section("B-20 communication disruption");
        Fixture f = new Fixture();
        f.colony.changeEnvironment(EnvironmentCondition.COMMUNICATION_DISRUPTION);
        SubmissionResult drone = f.colony.submitMission(f.exploration("M-1"),
                List.<Agent>of(f.layla, f.drone));
        Check.isTrue("B-20 drone mission rejected", !drone.isAccepted());
        Check.contains("B-20 drone reason", drone.getReason(), "communication disruption");
        RescueMission rescue = new RescueMission("M-2", "Technician", MartianSurface.INSTANCE,
                2, 30, new ResourceRequirement());
        SubmissionResult rescueResult = f.colony.submitMission(rescue,
                List.<Agent>of(f.salma, f.rover));
        Check.isTrue("B-20 rescue rejected", !rescueResult.isAccepted());
        Check.contains("B-20 rescue reason", rescueResult.getReason(), "communication disruption");
    }

    private static void testB21() {
        Check.section("B-21 rover battery runs out");
        Fixture f = new Fixture(12);
        ExplorationMission m = f.exploration("M-1", 4, 10);
        SubmissionResult result = f.colony.submitMission(m, f.explorationTeam());
        Check.isTrue("B-21 accepted at 12%", result.isAccepted());
        f.colony.tick();
        f.colony.tick();
        f.colony.tick();
        Check.equal("B-21 mission FAILED", MissionState.FAILED, m.getState());
        Check.contains("B-21 reason", m.getFailureReason(), "battery");
        Check.isTrue("B-21 agents released", !f.layla.isAssigned() && !f.rover.isAssigned());
    }

    private static void testB22() {
        Check.section("B-22 failActiveMission");
        Fixture f = new Fixture();
        Check.isTrue("B-22 no active mission gives false", !f.colony.failActiveMission("x"));
        ExplorationMission m = f.exploration("M-1");
        f.colony.submitMission(m, f.explorationTeam());
        f.colony.tick();
        f.rover.damage();
        Check.isTrue("B-22 returns true", f.colony.failActiveMission("x"));
        Check.equal("B-22 mission FAILED", MissionState.FAILED, m.getState());
        Check.equal("B-22 reason kept", "x", m.getFailureReason());
        Check.isTrue("B-22 damaged rover stays on the surface",
                f.rover.getLocation() == MartianSurface.INSTANCE);
        Check.isTrue("B-22 usable agent goes home", f.layla.getLocation() == f.habitatA);
        Check.isTrue("B-22 second call gives false", !f.colony.failActiveMission("y"));
    }

    private static void testB23() {
        Check.section("B-23 team coverage");
        Fixture f = new Fixture();
        SubmissionResult result = f.colony.submitMission(f.exploration("M-1"),
                List.<Agent>of(f.layla));
        Check.isTrue("B-23 rejected", !result.isAccepted());
        Check.contains("B-23 reason", result.getReason(), "SURFACE_TRAVEL");
    }

    private static void testB24() {
        Check.section("B-24 unavailable agent");
        Fixture f = new Fixture();
        f.layla.assignToMission();
        SubmissionResult result = f.colony.submitMission(f.exploration("M-1"),
                f.explorationTeam());
        Check.isTrue("B-24 rejected", !result.isAccepted());
        Check.contains("B-24 reason", result.getReason(), "not available");
    }

    /**
     * Runs every B test and prints the summary.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        testB01();
        testB02();
        testB03();
        testB04();
        testB05();
        testB06();
        testB07();
        testB08();
        testB09();
        testB10();
        testB11();
        testB12();
        testB13();
        testB14();
        testB15();
        testB16();
        testB17();
        testB18();
        testB19();
        testB20();
        testB21();
        testB22();
        testB23();
        testB24();
        Check.summary();
    }
}