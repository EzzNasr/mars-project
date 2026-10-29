package marsops.missions;

import java.util.ArrayList;
import java.util.List;
import marsops.common.MissionState;
import marsops.common.MissionType;
import marsops.common.ResourceRequirement;

public abstract class AbstractMission implements Mission {

    private final String id;
    private final MissionType type;
    private final int durationUnits;																				//	>=	1	else	IllegalArgumentException
    private MissionState state = MissionState.CREATED;
    private int progressUnits = 0;
    private String failureReason;
    //private final List<Agent> assignedAgents = new ArrayList<>();         //WAITING FOR A's COMMIT
    //private final List<Facility> targetFacilities;                        //WAITING FOR A's COMMIT
    //private final ResourceRequirement resourceRequirement;                //WAITING FOR A's COMMIT

    /**
     *
     * @param id
     * @param type
     * @param durationUnits
     * @param resourceRequirement
     * @param targetFacilities
     */
    protected AbstractMission(String id, MissionType type, int durationUnits, ResourceRequirement resourceRequirement){ 
        throw new UnsupportedOperationException("TODO");    // List<Facility> targetFacilities (ADD IT TO CONSTRACTOR)
    }                                                             //WAITING FOR A's COMMIT

    protected void onComplete() {}		// hook (TEMPLATE METHOD): called once when COMPLETED 
                                                // implements:getId, getType, getState, getDurationUnits, getProgressUnits, getFailureReason, getAssignedAgents,
                                                // getTargetFacilities, getResourceRequirement, assign, start, advance, fail, isFinished, describe 
                                                // subclasses implement the 8 "what agents and the colony ask" methods
}
