package Ref;

import java.io.Serializable;

/**
 *<h1>Ric</h1>
 * The Ric class is a template for a Reuters Instrument Code.
 * It contains variables for a Ric code, a name, as well as functions to split the Ric and extract the company name and exchange character.
 */
public class Ric implements Serializable{
	public String ric;
	public Ric(String ric){
		this.ric=ric;
	}
	public String getEx(){
		return ric.split(".")[1];
	}
	public String getCompany(){
		return ric.split(".")[0];
	}
}