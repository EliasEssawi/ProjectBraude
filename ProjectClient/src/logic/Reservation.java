package logic;

import java.sql.Timestamp;

/**
 * <p>
 * Represents a reservation made by a subscriber for a specific parking spot,
 * including the reservation time window and relevant metadata.
 * </p>
 * 
 * This class is used to manage and retrieve reservation information within the system,
 * especially for scheduling, validation, and reporting purposes.
 * 
 * @author Bahaa
 */
public class Reservation {

    /** Unique identifier for the reservation */
    private int reservationID;

    /** Subscriber associated with the reservation */
    private String subscriber;

    /** The parking spot reserved */
    private int spot;

    /** The start time of the reservation */
    private Timestamp startTime;

    /** The end time of the reservation */
    private Timestamp endTime;

    /**
     * Default constructor initializing fields with default values.
     */
    public Reservation() {
        this.reservationID = 0;
        this.subscriber = null;
        this.spot = 0;
        this.startTime = null;
        this.endTime = null;
    }

    /**
     * Constructs a Reservation object with the specified values.
     *
     * @param reservationID the unique reservation ID
     * @param subscriber    the subscriber who made the reservation
     * @param spot          the parking spot number
     * @param startTime     the start time of the reservation
     * @param endTime       the end time of the reservation
     */
    public Reservation(int reservationID, String subscriber, int spot,
                       Timestamp startTime, Timestamp endTime) {
        this.reservationID = reservationID;
        this.subscriber = subscriber;
        this.spot = spot;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** @return the unique reservation ID */
    public int getReservationID() {
        return reservationID;
    }

    /** @param reservationID the reservation ID to set */
    public void setReservationID(int reservationID) {
        this.reservationID = reservationID;
    }

    /** @return the subscriber who made the reservation */
    public String getSubscriber() {
        return subscriber;
    }

    /** @param subscriber the subscriber to assign */
    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    /** @return the parking spot number */
    public int getSpot() {
        return spot;
    }

    /** @param spot the parking spot to set */
    public void setSpot(int spot) {
        this.spot = spot;
    }

    /** @return the start time of the reservation */
    public Timestamp getStartTime() {
        return startTime;
    }

    /** @param startTime the start time to set */
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    /** @return the end time of the reservation */
    public Timestamp getEndTime() {
        return endTime;
    }

    /** @param endTime the end time to set */
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
}
