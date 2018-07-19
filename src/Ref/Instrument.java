package Ref;

import java.io.Serializable;
import java.util.Date;

/**
  <h1>Instrument</h1>
 The Instrument class is used to hold the template for all financial instruments used in this program.
 This class contains variables for a RIC, a name, id, isin, sedol and bbid.
 */
public class Instrument implements Serializable
{
	long id;
	String name;
	Ric ric;
	String isin;
	String sedol;
	String bbid;
	public Instrument(Ric ric){
		this.ric=ric;
	}
	public String toString(){
		return ric.ric;
	}
}

/**
 *<h1>EqInstrument</h1>
 */
class EqInstrument extends Instrument
{
	Date exDividend;

	public EqInstrument(Ric ric){
		super(ric);
	}
}

/**
 * <h1>FutInstrument</h1>
 */
class FutInstrument extends Instrument
{
	Date expiry;
	Instrument underlier;

	public FutInstrument(Ric ric){
		super(ric);
	}
}
/*TODO
Index
bond
methods
*/