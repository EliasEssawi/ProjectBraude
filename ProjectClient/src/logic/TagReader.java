package logic;

/**
 * <p>
 * Represents a Tag Reader device used in the parking system to associate a
 * physical reader with a specific subscriber.
 * </p>
 *
 * <p>
 * This class stores the identifier of the tag reader and the subscriber ID
 * it is linked to. It is typically used in automated entry systems to
 * validate and recognize subscribers upon entry or exit.
 * </p>
 * 
 * @author Bahaa
 */
public class TagReader {

    /** Unique identifier for the tag reader device */
    private int TagReaderID;

    /** ID of the subscriber associated with this tag reader */
    private String SubscriberID;

    /**
     * Constructs a TagReader object with the specified ID and associated subscriber.
     *
     * @param tagReaderID   the ID of the tag reader device
     * @param subscriberID  the ID of the subscriber associated with the tag reader
     */
    public TagReader(int tagReaderID, String subscriberID) {
        this.TagReaderID = tagReaderID;
        this.SubscriberID = subscriberID;
    }

    /**
     * Returns the ID of the tag reader device.
     *
     * @return the tag reader ID
     */
    public int getTagReaderID() {
        return TagReaderID;
    }

    /**
     * Sets the ID of the tag reader device.
     *
     * @param tagReaderID the tag reader ID to set
     */
    public void setTagReaderID(int tagReaderID) {
        this.TagReaderID = tagReaderID;
    }

    /**
     * Returns the ID of the subscriber associated with this tag reader.
     *
     * @return the subscriber ID
     */
    public String getSubscriberID() {
        return SubscriberID;
    }

    /**
     * Sets the ID of the subscriber associated with this tag reader.
     *
     * @param subscriberID the subscriber ID to set
     */
    public void setSubscriberID(String subscriberID) {
        this.SubscriberID = subscriberID;
    }

    /**
     * Returns a string representation of the tag reader.
     *
     * @return a formatted string with tag reader details
     */
    @Override
    public String toString() {
        return "TagReader{" +
                "TagReaderID=" + TagReaderID +
                ", SubscriberID='" + SubscriberID + '\'' +
                '}';
    }
}
