package logic;

import java.sql.Date;

/**
 * Represents a parking order placed by a subscriber.
 * Contains details such as parking space, order number,
 * confirmation code, subscriber ID, and relevant dates.
 *
 * @author Bahaa
 */
public class Order {

    /** The parking space reserved in the order. */
    private int parking_space;

    /** The unique identifier of the order. */
    private int order_number;

    /** The date the parking is reserved for. */
    private Date order_date;

    /** A confirmation code provided for the order. */
    private int confirmation_code;

    /** The ID of the subscriber who placed the order. */
    private int subscriber_id;

    /** The date the order was placed. */
    private Date date_of_placing_an_order;

    /**
     * Default constructor initializing fields with default values.
     */
    public Order() {
        this.parking_space = 0;
        this.order_number = 0;
        this.order_date = null;
        this.confirmation_code = 0;
        this.subscriber_id = 0;
        this.date_of_placing_an_order = null;
    }

    /**
     * Parameterized constructor for creating a fully initialized order.
     *
     * @param parking_space              The parking space number
     * @param order_number               The order's unique number
     * @param order_date                 The date the parking is reserved for
     * @param confirmation_code          The confirmation code of the order
     * @param subscriber_id              The subscriber who placed the order
     * @param date_of_placing_an_order   The date the order was placed
     */
    public Order(int parking_space, int order_number, Date order_date,
                 int confirmation_code, int subscriber_id, Date date_of_placing_an_order) {
        this.parking_space = parking_space;
        this.order_number = order_number;
        this.order_date = order_date;
        this.confirmation_code = confirmation_code;
        this.subscriber_id = subscriber_id;
        this.date_of_placing_an_order = date_of_placing_an_order;
    }

    /**
     * @return the parking space number
     */
    public int getParkingSpace() {
        return this.parking_space;
    }

    /**
     * @return the order number
     */
    public int getOrderNumber() {
        return this.order_number;
    }

    /**
     * @return the date the parking is reserved for
     */
    public Date getOrderDate() {
        return this.order_date;
    }

    /**
     * @return the confirmation code
     */
    public int getConfirmationCode() {
        return this.confirmation_code;
    }

    /**
     * @return the subscriber ID who placed the order
     */
    public int getSubscriberId() {
        return this.subscriber_id;
    }

    /**
     * @return the date the order was placed
     */
    public Date getDateOfPlacingAnOrder() {
        return this.date_of_placing_an_order;
    }

    /**
     * @param parking_space the parking space to set
     */
    public void setParkingSpace(int parking_space) {
        this.parking_space = parking_space;
    }

    /**
     * @param order_number the order number to set
     */
    public void setOrderNumber(int order_number) {
        this.order_number = order_number;
    }

    /**
     * @param order_date the order date to set
     */
    public void setOrderDate(Date order_date) {
        this.order_date = order_date;
    }

    /**
     * @param confirmation_code the confirmation code to set
     */
    public void setConfirmationCode(int confirmation_code) {
        this.confirmation_code = confirmation_code;
    }

    /**
     * @param subscriber_id the subscriber ID to set
     */
    public void setSubscriberId(int subscriber_id) {
        this.subscriber_id = subscriber_id;
    }

    /**
     * @param date_of_placing_an_order the date the order was placed to set
     */
    public void setDateOfPlacingAnOrder(Date date_of_placing_an_order) {
        this.date_of_placing_an_order = date_of_placing_an_order;
    }
}
