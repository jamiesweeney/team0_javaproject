package OrderClient;

import java.io.Serializable;

import Ref.Instrument;

public class NewOrderSingle implements Serializable{
	public int size;
	public float price;
	public Instrument instrument;
	public int side;
	public NewOrderSingle(int size,float price,Instrument instrument, int side){
		this.size=size;
		this.price=price;
		this.instrument=instrument;
		this.side=side;
	}
}