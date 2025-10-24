package logic;

/**
 * <p>
 * Represents a subscriber in the parking system.
 * Each subscriber is identified by a unique ID and contains contact details.
 * </p>
 * 
 * <p>
 * This class is used to store and manipulate subscriber-related information
 * such as name, phone number, and email.
 * </p>
 * 
 * @author Bahaa
 */
public class Subscriber {

    /** Unique ID associated with the subscriber */
    private String SubscriberID;

    /** The subscriber's full name */
    private String UserName;

    /** The subscriber's phone number */
    private String PhoneNumber;

    /** The subscriber's email address */
    private String Email;

    /**
     * Default constructor that initializes fields with default values.
     */
    public Subscriber() {
        this.SubscriberID = "-1";
        this.UserName = null;
        this.PhoneNumber = null;
        this.Email = null;
    }

    /**
     * Constructs a Subscriber object with the provided information.
     *
     * @param subscriberID the subscriber's unique ID
     * @param userName     the subscriber's full name
     * @param phoneNumber  the subscriber's phone number
     * @param email        the subscriber's email address
     */
    public Subscriber(String subscriberID, String userName, String phoneNumber, String email) {
        this.SubscriberID = subscriberID;
        this.UserName = userName;
        this.PhoneNumber = phoneNumber;
        this.Email = email;
    }

    /** @return the subscriber's ID */
    public String getSubscriberID() {
        return SubscriberID;
    }

    /** @param subscriberID the ID to set for the subscriber */
    public void setSubscriberID(String subscriberID) {
        this.SubscriberID = subscriberID;
    }

    /** @return the subscriber's name */
    public String getUserName() {
        return UserName;
    }

    /** @param userName the name to set for the subscriber */
    public void setUserName(String userName) {
        this.UserName = userName;
    }

    /** @return the subscriber's phone number */
    public String getPhoneNumber() {
        return PhoneNumber;
    }

    /** @param phoneNumber the phone number to set */
    public void setPhoneNumber(String phoneNumber) {
        this.PhoneNumber = phoneNumber;
    }

    /** @return the subscriber's email address */
    public String getEmail() {
        return Email;
    }

    /** @param email the email address to set */
    public void setEmail(String email) {
        this.Email = email;
    }

    /**
     * Returns a string representation of the subscriber for debugging/logging.
     *
     * @return a string containing subscriber details
     */
    @Override
    public String toString() {
        return "Subscriber{" +
                "SubscriberID='" + SubscriberID + '\'' +
                ", UserName='" + UserName + '\'' +
                ", PhoneNumber='" + PhoneNumber + '\'' +
                ", Email='" + Email + '\'' +
                '}';
    }
}
