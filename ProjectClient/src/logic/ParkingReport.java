package logic;

import java.sql.Timestamp;
import java.sql.Time;

/**
 * <p>
 * Represents a parking report containing statistical data about parking activity
 * within a specific period. This includes details such as the number of late returns,
 * extensions, most common parking duration, and total orders placed.
 * </p>
 * 
 * This class is used for generating and storing parking report information
 * that can be analyzed by system administrators or managers.
 * 
 * @author Bahaa
 */
public class ParkingReport {

    /** Unique identifier for the report */
    private int noReport;

    /** Timestamp indicating when the report was created */
    private Timestamp dateOfReport;

    /** Total number of late parking instances recorded in the report */
    private int totalLate;

    /** Total number of parking time extensions recorded in the report */
    private int totalExtensions;

    /** The most common duration of parking during the reporting period */
    private Time mostCommonTime;

    /** Number of orders placed during the reporting period */
    private int howManyOrdersWerePlaced;

    /**
     * Default constructor initializing all fields to default values.
     */
    public ParkingReport() {
        this.noReport = 0;
        this.dateOfReport = null;
        this.totalLate = 0;
        this.totalExtensions = 0;
        this.mostCommonTime = null;
        this.howManyOrdersWerePlaced = 0;
    }

    /**
     * Parameterized constructor for creating a parking report with specific values.
     *
     * @param noReport                the report ID
     * @param dateOfReport            the timestamp of report generation
     * @param totalLate               total number of late returns
     * @param totalExtensions         total number of time extensions
     * @param mostCommonTime          most common parking duration
     * @param howManyOrdersWerePlaced total number of orders placed
     */
    public ParkingReport(int noReport, Timestamp dateOfReport, int totalLate,
                         int totalExtensions, Time mostCommonTime, int howManyOrdersWerePlaced) {
        this.noReport = noReport;
        this.dateOfReport = dateOfReport;
        this.totalLate = totalLate;
        this.totalExtensions = totalExtensions;
        this.mostCommonTime = mostCommonTime;
        this.howManyOrdersWerePlaced = howManyOrdersWerePlaced;
    }

    /** @return the unique report number */
    public int getNoReport() {
        return noReport;
    }

    /** @param noReport the report number to set */
    public void setNoReport(int noReport) {
        this.noReport = noReport;
    }

    /** @return the timestamp when the report was created */
    public Timestamp getDateOfReport() {
        return dateOfReport;
    }

    /** @param dateOfReport the report creation date to set */
    public void setDateOfReport(Timestamp dateOfReport) {
        this.dateOfReport = dateOfReport;
    }

    /** @return the total number of late returns in this report */
    public int getTotalLate() {
        return totalLate;
    }

    /** @param totalLate the number of late returns to set */
    public void setTotalLate(int totalLate) {
        this.totalLate = totalLate;
    }

    /** @return the total number of parking extensions in this report */
    public int getTotalExtensions() {
        return totalExtensions;
    }

    /** @param totalExtensions the number of extensions to set */
    public void setTotalExtensions(int totalExtensions) {
        this.totalExtensions = totalExtensions;
    }

    /** @return the most common parking time during the reporting period */
    public Time getMostCommonTime() {
        return mostCommonTime;
    }

    /** @param mostCommonTime the most common parking time to set */
    public void setMostCommonTime(Time mostCommonTime) {
        this.mostCommonTime = mostCommonTime;
    }

    /** @return how many orders were placed in this reporting period */
    public int getHowManyOrdersWerePlaced() {
        return howManyOrdersWerePlaced;
    }

    /** @param howManyOrdersWerePlaced number of orders to set */
    public void setHowManyOrdersWerePlaced(int howManyOrdersWerePlaced) {
        this.howManyOrdersWerePlaced = howManyOrdersWerePlaced;
    }
}
