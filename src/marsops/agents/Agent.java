package marsops.agents;

import java.util.Set;
import marsops.common.Capability;
import marsops.common.EmergencyType;
import marsops.common.Location;
import marsops.missions.Mission;

/** 

can be assigned to missions and emergencies (humans and robots)
 
    * @author EzzNasr
 */
public interface Agent {
    String getId();
    String getName();
    Location getLocation();
    void setLocation(Location location);
    Location getHomeLocation();
    Set<Capability> getCapabilities();
    default boolean hasCapability(Capability c) { return getCapabilities().contains(c); }

    String getUnusableReason();
    default boolean isUsable() { return getUnusableReason() == null; }
    boolean isAssigned();
    void assignToMission();
    void release();
    default boolean isAvailable() { return isUsable() && !isAssigned(); }

    /**
      **FIXME i need to have the missions file 
     */
    String getCannotPerformReason(Mission mission);
    default boolean canPerform(Mission mission) { return getCannotPerformReason(mission) == null; }

    boolean canHandle(EmergencyType type);
    boolean isRemoteControlled();
    void takeDamage(int amount);
    void recover(int amount);
    void tick();
    String describe();
}