package OrderRouter;

import java.io.IOException;
import Ref.Instrument;


/** An interface defining how the routers should act.
 */
public interface Router
{
	// Not sure what this does
	enum api{routeOrder, sendCancel, priceAtSize};


	/** Routes the order to the router for selling/buying
	 *
	 * @param id the OM order id that we are routing
	 * @param sliceId the slice for that order that we are routing
	 * @param size the size that we are routing
	 * @param i the instrument object that we are routing
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void routeOrder(int id,
						   int sliceId,
						   int size,
						   Instrument i)
			               throws IOException, InterruptedException;

	/** Determines the price for an order to be routed and sends a message
	 * back to the order manager letting it know.
	 *
	 * @param id the OM order id
	 * @param sliceId the slice id of the order
	 * @param i the instrument that we are asking for
	 * @param size the size that we are routing
	 * @throws IOException
	 */
	void priceAtSize(int id,
							int sliceId,
							Instrument i,
							int size)
			                throws IOException;
}
