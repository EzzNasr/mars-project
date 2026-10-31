package marsops.emergencies;

import java.util.List;

import marsops.agents.Agent;
import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.FacilityStatus;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Laboratory;

// A lab is on fire. Fixed: the lab is damaged. Not fixed: the lab is destroyed.
public class Fire extends Emergency {
    private static final int DAMAGE = 30;
    private static final int RECOVERY = 20;
    private static final int OXYGEN_BURNED = 5;

    private final Laboratory lab;

    public Fire(String id, Laboratory lab) {
        super(id, EmergencyType.FIRE, lab);
        this.lab = lab;
    }

    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.MEDICAL);
    }

    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.WATER, 10)
                .with(ResourceType.MEDICAL_SUPPLIES, 1)
                .with(ResourceType.BATTERY_POWER, 3);
    }

    // the lab can't be used, the equipment breaks and everybody inside gets hurt
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        lab.setOnFire(true);
        lab.setStatus(FacilityStatus.UNAVAILABLE);
        lab.damageAllEquipment();
        lab.lose(ResourceType.OXYGEN, OXYGEN_BURNED);
        for (Agent agent : colony.getAgentsAt(lab)) {
            agent.takeDamage(DAMAGE);
        }
    }

    @Override
    protected void applyResolution(MarsColony colony) {
        lab.setOnFire(false);
        lab.setStatus(FacilityStatus.DAMAGED);
        for (Agent agent : colony.getAgentsAt(lab)) {
            agent.recover(RECOVERY);
        }
    }

    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
        lab.setStatus(FacilityStatus.DESTROYED);
    }
}
