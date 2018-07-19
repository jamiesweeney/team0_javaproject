package OrderClient;

import java.io.Serializable;

import Ref.Instrument;
/**
 * <h1>NewOrderSingle</h1>
 * NewOrderSingle implements Serializable, and is used to hold the details of a new order, before it is
 * converted to an actual Order object.
 *
 * */
public class NewOrderSingle implements Serializable{

	// Instance variables
	public int size;
	public float price;
	public Instrument instrument;
	public int side;

	/** Constructor for new order single
	 *
	 * @param size size of the order requested
	 * @param price price of the stock requested
	 * @param instrument the instrument object that is requested
	 * @param side 1 for buy 2 for sell
	 */
	public NewOrderSingle(int size,float price,Instrument instrument, int side)
	{
		this.size=size;
		this.price=price;
		this.instrument=instrument;
		this.side=side;
	}
}