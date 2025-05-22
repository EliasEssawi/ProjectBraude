package logic;

import java.sql.Date;

public class Order {
	private int parking_space;
	private int order_number;
	private Date order_date;
	private int confirmation_code;
	private int subscriber_id;
	private Date date_of_placing_an_order;
	
	public Order()
	{
		this.parking_space = 0;
		this.order_number = 0;
		this.order_date = null;
		this.confirmation_code = 0;
		this.subscriber_id = 0;
		this.date_of_placing_an_order = null;
	}
	
	public Order(int parking_space, int order_number, Date order_date,
            	int confirmation_code, int subscriber_id, Date date_of_placing_an_order)
	{
		this.parking_space = parking_space;
		this.order_number = order_number;
		this.order_date = order_date;
		this.confirmation_code = confirmation_code;
		this.subscriber_id = subscriber_id;
		this.date_of_placing_an_order = date_of_placing_an_order;
	}
	
	public int getParkingSpace()
	{
		return this.parking_space;
	}
	
	public int getOrderNumber()
	{
		return this.order_number;
	}
	
	public Date getOrderDate()
	{
		return this.order_date;
	}
	
	public int getConfirmationCode()
	{
		return this.confirmation_code;
	}
	
	public int getSubscriberId()
	{
		return this.subscriber_id;
	}
	
	public Date getDateOfPlacingAnOrder()
	{
		return this.date_of_placing_an_order;
	}
	
	public void setParkingSpace(int parking_space) {
	    this.parking_space = parking_space;
	}

	public void setOrderNumber(int order_number) {
	    this.order_number = order_number;
	}

	public void setOrderDate(Date order_date) {
	    this.order_date = order_date;
	}

	public void setConfirmationCode(int confirmation_code) {
	    this.confirmation_code = confirmation_code;
	}

	public void setSubscriberId(int subscriber_id) {
	    this.subscriber_id = subscriber_id;
	}

	public void setDateOfPlacingAnOrder(Date date_of_placing_an_order) {
	    this.date_of_placing_an_order = date_of_placing_an_order;
	}
}
