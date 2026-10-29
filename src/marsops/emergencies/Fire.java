package marsops.emergencies;

import java.util.List;

import marsops.agents.Agent; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.FacilityStatus;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Laboratory; // waiting for A's commit

// Fire in a laboratory (D-14).
// Solved: lab becomes DAMAGED. Not solved: lab becomes DESTROYED.
public class Fire extends Emergency {
    // damage for every agent inside the lab
    public static final int AGENT_DAMAGE = 30;
    // health the agents get back when the fire is out
    public static final int AGENT_RECOVERY = 20;
    // oxygen that burns from the lab stock
    public static final int OXYGEN_BURNED = 5;

    private final Laboratory lab;

    public Fire(String id, Laboratory lab) {
        super(id, EmergencyType.FIRE, requireLab(lab));
        this.lab = lab;
    }

    // used before super() so the lab is checked first
    private static Laboratory requireLab(Laboratory lab) {
        if (lab == null) {
            throw new IllegalArgumentException("lab required");
        }
        return lab;
    }

    // needs an engineer and a medical officer
    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.MEDICAL);
    }

    // 10 water, 1 medical supplies and 3 battery power
    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.WATER, 10)
                .with(ResourceType.MEDICAL_SUPPLIES, 1)
                .with(ResourceType.BATTERY_POWER, 3);
    }

    // lab is on fire: unavailable, equipment damaged, oxygen burns, people inside get hurt
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        lab.setOnFire(true); // waiting for A's commit
        lab.setStatus(FacilityStatus.UNAVAILABLE); // waiting for A's commit
        lab.damageAllEquipment(); // waiting for A's commit
        lab.lose(ResourceType.OXYGEN, OXYGEN_BURNED); // waiting for A's commit
        for (Agent agent : colony.getAgentsAt(lab)) { // waiting for B's commit
            agent.takeDamage(AGENT_DAMAGE); // waiting for A's commit
        }
    }

    // fire is out: lab is DAMAGED and the people inside recover
    @Override
    protected void applyResolution(MarsColony colony) {
        lab.setOnFire(false); // waiting for A's commit
        lab.setStatus(FacilityStatus.DAMAGED); // waiting for A's commit
        for (Agent agent : colony.getAgentsAt(lab)) { // waiting for B's commit
            agent.recover(AGENT_RECOVERY); // waiting for A's commit
        }
    }

    // nobody stopped the fire, so the lab is DESTROYED
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
        lab.setStatus(FacilityStatus.DESTROYED); // waiting for A's commit
    }
}
