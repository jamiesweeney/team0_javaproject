package TradeScreen;

import java.io.IOException;

import OrderManager.Order;

public interface TradeScreen {
	enum api{newOrder,price,fill,cross};

	/** Sends a new order to the trade screen.
	 *
	 * @param id the id of the OM order
	 * @param order the order object itself
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void newOrder(int id,Order order) throws IOException, InterruptedException;

	/** 
	 *
	 * @param id
	 * @throws IOException
	 */
	void acceptOrder(int id) throws IOException;
	void sliceOrder(int id,int sliceSize) throws IOException;
	void price(int id,Order o) throws InterruptedException, IOException;
}
