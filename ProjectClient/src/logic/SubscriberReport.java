package logic;

import java.sql.Date;

/**
 * <p>
 * Represents a report for a specific subscriber, including parking statistics
 * such as late arrivals, late departures, and total parking duration.
 * </p>
 *
 * <p>
 * This class is typically used to analyze subscriber behavior and generate
 * reports for system monitoring or administrative evaluation.
 * </p>
 * 
 * @author Bahaa
 */
public class SubscriberReport {

    /** Report number identifying this specific report entry */
    private int NoReport;

    /** ID of the subscriber the report is associated with */
    private String SubscriberID;

    /** The date on which the report was generated */
    private Date DateOfReport;

    /** The number of times the subscriber arrived late */
    private int CountLateArrive;

    /** The number of times the subscriber left late */
    private int CountLateLeave;

    /** The total parking duration in minutes/hours (depending on usage context) */
    private long TotalParkingDuration;

    /**
     * Constructs a SubscriberReport with all data fields initialized.
     *
     * @param noReport             the report number
     * @param subscriberID         the ID of the subscriber
     * @param dateOfReport         the date the report was created
     * @param countLateArrive      the number of late arrivals
     * @param countLateLeave       the number of late departures
     * @param totalParkingDuration the total parking duration
     */
    public SubscriberReport(int noReport, String subscriberID, Date dateOfReport,
                            int countLateArrive, int countLateLeave, long totalParkingDuration) {
        this.NoReport = noReport;
        this.SubscriberID = subscriberID;
        this.DateOfReport = dateOfReport;
        this.CountLateArrive = countLateArrive;
        this.CountLateLeave = countLateLeave;
        this.TotalParkingDuration = totalParkingDuration;
    }

    /** @return the report number */
    public int getNoReport() {
        return NoReport;
    }

    /** @param noReport the report number to set */
    public void setNoReport(int noReport) {
        this.NoReport = noReport;
    }

    /** @return the subscriber's ID */
    public String getSubscriberID() {
        return SubscriberID;
    }

    /** @param subscriberID the ID to set for the subscriber */
    public void setSubscriberID(String subscriberID) {
        this.SubscriberID = subscriberID;
    }

    /** @return the date the report was created */
    public Date getDateOfReport() {
        return DateOfReport;
    }

    /** @param dateOfReport the date of the report to set */
    public void setDateOfReport(Date dateOfReport) {
        this.DateOfReport = dateOfReport;
    }

    /** @return the number of late arrivals */
    public int getCountLateArrive() {
        return CountLateArrive;
    }

    /** @param countLateArrive the count of late arrivals to set */
    public void setCountLateArrive(int countLateArrive) {
        this.CountLateArrive = countLateArrive;
    }

    /** @return the number of late departures */
    public int getCountLateLeave() {
        return CountLateLeave;
    }

    /** @param countLateLeave the count of late departures to set */
    public void setCountLateLeave(int countLateLeave) {
        this.CountLateLeave = countLateLeave;
    }

    /** @return the total parking duration */
    public long getTotalParkingDuration() {
        return TotalParkingDuration;
    }

    /** @param totalParkingDuration the total parking duration to set */
    public void setTotalParkingDuration(long totalParkingDuration) {
        this.TotalParkingDuration = totalParkingDuration;
    }

    /**
     * Returns a string representation of the subscriber report.
     *
     * @return a string containing report details
     */
    @Override
    public String toString() {
        return "SubscriberReport{" +
                "NoReport=" + NoReport +
                ", SubscriberID='" + SubscriberID + '\'' +
                ", DateOfReport=" + DateOfReport +
                ", CountLateArrive=" + CountLateArrive +
                ", CountLateLeave=" + CountLateLeave +
                ", TotalParkingDuration=" + TotalParkingDuration +
                '}';
    }
}
