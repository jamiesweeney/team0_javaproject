package OrderClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import OrderManager.Order;
import Ref.Instrument;
/**
 * <h1>Client</h1>
 *
 * Client is an interface used to define interaction between the client(s) and the order manager.
 *
 * */
public interface Client{



	//Outgoing messages


	/** Used to send an order from the client to the order manager.
	 *
	 * @param id the id assigned to the order, unique for the client only
	 * @param size the amount of stock to buy
	 * @param msgType the message type tag 35
	 * @param price the requested price
	 * @param ins the instument object to buy
	 * @param side 1 for buy, 2 for sell
	 * @return
	 * @throws IOException
	 */
	int sendOrder(int id, int size,char msgType, float price, Instrument ins, int side)throws IOException;

	/** Allows the client to potentially cancel an order. if it has not been fufilled.
	 *
	 * @param id  the id of the order you want to cancel, unique to all order manager orders
	 */
	void sendCancel(int id);

	/** Connected the client to the order mananger
	 *
	 * @param serverSocket the socket that client will connect over
	 * @return
	 */
	Socket connect(InetSocketAddress serverSocket);

	
	//Incoming messages
	void partialFill(Order order);
	void fullyFilled(Order order);
	void cancelled(Order order);


	/** Handles incoming messages from the order manager
	 *
	 * @return
	 */
	void messageHandler();
}