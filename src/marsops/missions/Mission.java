package marsops.missions;

import java.util.List;
import java.util.Set;
import marsops.common.Capability;
import marsops.common.EnvironmentCondition;
import marsops.common.MissionState;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;
import marsops.common.Skill;



public	interface Mission{
    String	getId();
    MissionType getType();
    MissionState getState();
    int getDurationUnits();
    int getProgressUnits();
    String getFailureReason();																									//	null	unless	FAILED

    //List<Agent> getAssignedAgents();					//WAITING FOR A's COMMIT														//	unmodifiable;	empty	until	ASSIGNED

    //List<Facility> getTargetFacilities();		//may be empty (e.g. exploration); unmodifiable //WAITING FOR A's COMMIT

    ResourceRequirement getResourceRequirement();
    //	---	what	agents	and	the	colony	ask	--

    Set<Capability> getRequiredCapabilities();		//the TEAM must	cover all of them

    Skill getRequiredSkill();																										//	null	if	none

    int getMinBatteryPercent();																								//	0	if	none

    List<String> getRequiredEquipment();		// equipmentNAMES, may be empty

    boolean requiresCommunication();

    boolean requiresSurface();

    /**
     * null if the mission may start now; otherwise the	reason	(environment, target,facility,state...).
     */
    String getStartBlockReason(EnvironmentCondition env);

    /**
     * null if a mission already ASSIGNED / IN_PROGRESSmaycontinue;otherwise
     * why	it must fail.
     */
    String getInProgressFailureReason(EnvironmentCondition env);
    //	---	lifecycle:	called	ONLY	by	MarsColony	--

    //void assign(List<Agent> agents);			// CREATED ->ASSIGNED    //WAITING FOR A's COMMIT

    void start();					// ASSIGNED -> IN_PROGRESS

    void advance();					// IN_PROGRESS: progress+1; if progress >= duration ->  COMPLETED (calls	onComplete())

    void fail(String reason);				// CREATED/ASSIGNED/IN_PROGRESS -> FAILED

    boolean isFinished();				// COMPLETED or FAILED

    String describe();					// "M-1 [EXPLORATION] IN_PROGRESS 2/3"
}
