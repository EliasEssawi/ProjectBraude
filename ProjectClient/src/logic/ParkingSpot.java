package logic;

import java.sql.Timestamp;

/**
 * <p>
 * Represents a parking spot in the system, including information about its usage status,
 * assigned subscriber, and time details of the current or last parking session.
 * </p>
 * 
 * This class is typically used to track and manage the state of parking spots
 * in real-time or for generating reports on parking usage.
 * 
 * @author Bahaa
 */
public class ParkingSpot {

    /** Unique identifier for the parking spot */
    private int spotID;

    /** Subscriber associated with this parking spot (if any) */
    private String subscriber;

    /** Indicates whether the parking spot is currently in use */
    private boolean inUse;

    /** Timestamp marking the start of the current or last usage */
    private Timestamp startTime;

    /** Timestamp marking the end of the current or last usage */
    private Timestamp endTime;

    /**
     * Default constructor initializing fields with default values.
     */
    public ParkingSpot() {
        this.spotID = 0;
        this.subscriber = null;
        this.inUse = false;
        this.startTime = null;
        this.endTime = null;
    }

    /**
     * Constructs a ParkingSpot object with specific values.
     *
     * @param spotID     the unique spot identifier
     * @param subscriber the subscriber assigned to the spot
     * @param inUse      whether the spot is currently in use
     * @param startTime  the start time of usage
     * @param endTime    the end time of usage
     */
    public ParkingSpot(int spotID, String subscriber, boolean inUse, Timestamp startTime, Timestamp endTime) {
        this.spotID = spotID;
        this.subscriber = subscriber;
        this.inUse = inUse;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** @return the ID of the parking spot */
    public int getSpotID() {
        return spotID;
    }

    /** @param spotID the parking spot ID to set */
    public void setSpotID(int spotID) {
        this.spotID = spotID;
    }

    /** @return the subscriber assigned to this spot */
    public String getSubscriber() {
        return subscriber;
    }

    /** @param subscriber the subscriber name or ID to set */
    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    /** @return true if the spot is in use, false otherwise */
    public boolean isInUse() {
        return inUse;
    }

    /** @param inUse the in-use status to set */
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    /** @return the start time of the parking usage */
    public Timestamp getStartTime() {
        return startTime;
    }

    /** @param startTime the start time to set */
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    /** @return the end time of the parking usage */
    public Timestamp getEndTime() {
        return endTime;
    }

    /** @param endTime the end time to set */
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
}
