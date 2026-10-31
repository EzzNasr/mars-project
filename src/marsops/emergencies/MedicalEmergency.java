package marsops.emergencies;

import java.util.List;

import marsops.agents.Agent;
import marsops.agents.CrewMember;
import marsops.colony.MarsColony; // B's class
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Facility;

// A crew member gets hurt and needs a medical officer.
public class MedicalEmergency extends Emergency {
    private static final int RECOVERY = 40;

    private final CrewMember patient;
    private final int severity;

    public MedicalEmergency(String id, CrewMember patient, int severity) {
        super(id, EmergencyType.MEDICAL_EMERGENCY, patient.getLocation());
        if (severity <= 0) {
            throw new IllegalArgumentException("severity must be more than 0");
        }
        this.patient = patient;
        this.severity = severity;
    }

    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.MEDICAL);
    }

    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.MEDICAL_SUPPLIES, 2)
                .with(ResourceType.OXYGEN, 5);
    }

    @Override
    protected void applyInitialEffects(MarsColony colony) {
        patient.takeDamage(severity);
    }

    @Override
    protected void applyResolution(MarsColony colony) {
        patient.recover(RECOVERY);
    }

    // if the patient is not treated nothing else happens
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
    }

    // we can't treat anybody inside a facility that is not working
    @Override
    protected String checkPreconditions(MarsColony colony) {
        if (getLocation() instanceof Facility) {
            Facility facility = (Facility) getLocation();
            if (!facility.isOperational()) {
                return "Facility " + facility.getName() + " is not operational";
            }
        }
        return null;
    }

    // the patient can't treat himself
    @Override
    protected boolean isExcluded(Agent agent) {
        return agent == patient;
    }
}
