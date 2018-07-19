package OrderClient;

import java.io.Serializable;

import Ref.Instrument;
/**
 * <h1>NewOrderSingle</h1>
 * NewOrderSingle implements Serializable, and is used to hold the details of a new order.
 * */
public class NewOrderSingle implements Serializable{

	// Instance variables
	public int size;
	public float price;
	public Instrument instrument;
	public int side;

	/**
	 *NewOrderSingle() is teh constructor of the class and is basically just a setter.
	 * It takes in arguments and sets its internal values to them.
	 */
	public NewOrderSingle(int size,float price,Instrument instrument, int side)
	{
		this.size=size;
		this.price=price;
		this.instrument=instrument;
		this.side=side;
	}
}