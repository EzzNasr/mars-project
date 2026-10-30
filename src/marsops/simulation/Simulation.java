package marsops.simulation;

import java.util.ArrayList;
import java.util.List;

import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.EnvironmentCondition;
import marsops.emergencies.Emergency;
import marsops.emergencies.EmergencyManager;
import marsops.report.ColonyReport; // waiting for A's commit

// The simulation clock. It only moves time forward.
// Nothing is random: changes and emergencies are scheduled or triggered by hand.
public class Simulation {
    private final MarsColony colony;
    private final EmergencyManager emergencyManager;
    private int currentTime = 0;
    private final List<ScheduledEnvChange> envChanges = new ArrayList<>();
    private final List<ScheduledEmergency> emergencies = new ArrayList<>();

    // the manager is created here (composition)
    public Simulation(MarsColony colony) {
        if (colony == null) {
            throw new IllegalArgumentException("colony required");
        }
        this.colony = colony;
        this.emergencyManager = new EmergencyManager(colony);
    }

    public MarsColony getColony() {
        return colony;
    }

    public EmergencyManager getEmergencyManager() {
        return emergencyManager;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    // the change happens one time at the start of time unit atTime
    public void scheduleEnvironmentChange(int atTime, EnvironmentCondition condition) {
        if (atTime < 1) {
            throw new IllegalArgumentException("atTime must be >= 1");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition required");
        }
        envChanges.add(new ScheduledEnvChange(atTime, condition));
    }

    // the emergency is handled one time after the colony tick of time unit atTime
    public void scheduleEmergency(int atTime, Emergency emergency) {
        if (atTime < 1) {
            throw new IllegalArgumentException("atTime must be >= 1");
        }
        if (emergency == null) {
            throw new IllegalArgumentException("emergency required");
        }
        emergencies.add(new ScheduledEmergency(atTime, emergency));
    }

    // handle an emergency right now, time does not move
    public void triggerEmergency(Emergency emergency) {
        emergencyManager.handle(emergency);
    }

    // move time forward by some units
    public void advanceTime(int units) {
        if (units < 0) {
            throw new IllegalArgumentException("units must be >= 0");
        }
        for (int i = 0; i < units; i++) {
            advanceOneUnit();
        }
    }

    // order: time + 1, environment changes, colony tick, emergencies
    public void advanceOneUnit() {
        currentTime++;
        for (ScheduledEnvChange change : envChanges) {
            if (!change.fired && change.atTime == currentTime) {
                change.fired = true;
                colony.changeEnvironment(change.condition); // waiting for B's commit
            }
        }
        colony.tick(); // waiting for B's commit
        for (ScheduledEmergency scheduled : emergencies) {
            if (!scheduled.fired && scheduled.atTime == currentTime) {
                scheduled.fired = true;
                emergencyManager.handle(scheduled.emergency);
            }
        }
    }

    // the text report of the colony at the current time
    public String report() {
        return ColonyReport.build(colony, currentTime, emergencyManager.getHistory()); // waiting for A's commit
    }

    // an environment change that is waiting for its time
    private static class ScheduledEnvChange {
        private final int atTime;
        private final EnvironmentCondition condition;
        private boolean fired = false;

        ScheduledEnvChange(int atTime, EnvironmentCondition condition) {
            this.atTime = atTime;
            this.condition = condition;
        }
    }

    // an emergency that is waiting for its time
    private static class ScheduledEmergency {
        private final int atTime;
        private final Emergency emergency;
        private boolean fired = false;

        ScheduledEmergency(int atTime, Emergency emergency) {
            this.atTime = atTime;
            this.emergency = emergency;
        }
    }
}
