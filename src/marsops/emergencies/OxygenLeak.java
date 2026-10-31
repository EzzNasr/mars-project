package marsops.emergencies;

import java.util.List;

import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Habitat;

// The habitat loses oxygen. If we fix it we only lose it once,
// if we don't, we lose the same amount again.
public class OxygenLeak extends Emergency {
    private final Habitat habitat;
    private final int oxygenLoss;

    public OxygenLeak(String id, Habitat habitat, int oxygenLoss) {
        super(id, EmergencyType.OXYGEN_LEAK, habitat);
        if (oxygenLoss <= 0) {
            throw new IllegalArgumentException("oxygenLoss must be more than 0");
        }
        this.habitat = habitat;
        this.oxygenLoss = oxygenLoss;
    }

    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR);
    }

    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.SPARE_PARTS, 2)
                .with(ResourceType.BATTERY_POWER, 5);
    }

    @Override
    protected void applyInitialEffects(MarsColony colony) {
        habitat.setLeaking(true);
        habitat.loseOxygen(oxygenLoss);
    }

    @Override
    protected void applyResolution(MarsColony colony) {
        habitat.setLeaking(false);
    }

    // still leaking, so the same amount goes away again
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
        habitat.loseOxygen(oxygenLoss);
    }
}
