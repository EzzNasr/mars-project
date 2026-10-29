package marsops.emergencies;

import java.util.List;

import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Habitat; // waiting for A's commit

// Oxygen leak in a habitat (D-13).
// Oxygen is lost when detected. If nobody fixes it, the same amount is lost again.
public class OxygenLeak extends Emergency {
    private final Habitat habitat;
    private final int oxygenLoss;

    public OxygenLeak(String id, Habitat habitat, int oxygenLoss) {
        super(id, EmergencyType.OXYGEN_LEAK, requireHabitat(habitat));
        if (oxygenLoss <= 0) {
            throw new IllegalArgumentException("oxygenLoss must be > 0");
        }
        this.habitat = habitat;
        this.oxygenLoss = oxygenLoss;
    }

    // used before super() so the habitat is checked first
    private static Habitat requireHabitat(Habitat habitat) {
        if (habitat == null) {
            throw new IllegalArgumentException("habitat required");
        }
        return habitat;
    }

    // needs an engineer and a repair robot
    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.ENGINEERING, Capability.ROBOTIC_REPAIR);
    }

    // 2 spare parts and 5 battery power
    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.SPARE_PARTS, 2)
                .with(ResourceType.BATTERY_POWER, 5);
    }

    // the habitat starts leaking and loses oxygen the first time
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        habitat.setLeaking(true); // waiting for A's commit
        habitat.loseOxygen(oxygenLoss); // waiting for A's commit
    }

    // leak fixed, so the total loss is only one time
    @Override
    protected void applyResolution(MarsColony colony) {
        habitat.setLeaking(false); // waiting for A's commit
    }

    // not fixed, so the same oxygen is lost a second time (leaking stays true)
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
        habitat.loseOxygen(oxygenLoss); // waiting for A's commit
    }
}
