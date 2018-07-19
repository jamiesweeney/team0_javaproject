package LiveMarketData;

import OrderManager.Order;
import Ref.Instrument;
/**
 * <h1>LiveMarketData</h1>
 * The LiveMarketData interface is used by other classes through implementation.
 * */
public interface LiveMarketData
{
	/**
	 * LiveMarketData only has one function, setPrice(Order o).
	 * */
	public void setPrice(Order o);
}
