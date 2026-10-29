package marsops.emergencies;

import java.util.List;

import marsops.agents.Agent; // waiting for A's commit
import marsops.agents.CrewMember; // waiting for A's commit
import marsops.colony.MarsColony; // waiting for B's commit
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.ResourceRequirement;
import marsops.common.ResourceType;
import marsops.facilities.Facility; // waiting for A's commit

// Medical emergency (D-16).
// The patient gets hurt and recovers if a medical officer treats them.
public class MedicalEmergency extends Emergency {
    // health the patient gets back after treatment
    public static final int RECOVERY = 40;

    private final CrewMember patient;
    private final int severity;

    // the location is where the patient is when the emergency is created
    public MedicalEmergency(String id, CrewMember patient, int severity) {
        super(id, EmergencyType.MEDICAL_EMERGENCY, requirePatient(patient).getLocation());
        if (severity <= 0) {
            throw new IllegalArgumentException("severity must be > 0");
        }
        this.patient = patient;
        this.severity = severity;
    }

    // used before super() so the patient is checked first
    private static CrewMember requirePatient(CrewMember patient) {
        if (patient == null) {
            throw new IllegalArgumentException("patient required");
        }
        return patient;
    }

    // needs only a medical officer
    @Override
    public List<Capability> getRequiredResponderCapabilities() {
        return List.of(Capability.MEDICAL);
    }

    // 2 medical supplies and 5 oxygen
    @Override
    public ResourceRequirement getResponseResources() {
        return new ResourceRequirement()
                .with(ResourceType.MEDICAL_SUPPLIES, 2)
                .with(ResourceType.OXYGEN, 5);
    }

    // the patient takes damage equal to the severity
    @Override
    protected void applyInitialEffects(MarsColony colony) {
        patient.takeDamage(severity); // waiting for A's commit
    }

    // the patient is treated and gets health back
    @Override
    protected void applyResolution(MarsColony colony) {
        patient.recover(RECOVERY); // waiting for A's commit
    }

    // nothing happens if the patient is not treated
    @Override
    protected void applyUnresolvedPenalty(MarsColony colony) {
    }

    // if the facility is not operational we cannot treat the patient
    @Override
    protected String checkPreconditions(MarsColony colony) {
        if (getLocation() instanceof Facility) {
            Facility facility = (Facility) getLocation();
            if (!facility.isOperational()) { // waiting for A's commit
                return "Facility " + facility.getName() + " is not operational";
            }
        }
        return null;
    }

    // the patient cannot treat themselves
    @Override
    protected boolean isExcluded(Agent agent) {
        return agent == patient;
    }
}
