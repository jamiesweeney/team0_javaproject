package OrderClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import OrderManager.Order;
import Ref.Instrument;
/**
 * <h1>Client</h1>
 *
 * Client is an interface implemented by SampleClient.
 *
 * */
public interface Client{
	//Outgoing messages
	int sendOrder(int id, int size,char msgType, float price, Instrument ins, int side)throws IOException;
	/**
	 * @author David Hesketh
	 */
	void sendCancel(int id);

	/**
	 * @author: Oliver Morrison
	 * */
	Socket connect(InetSocketAddress serverSocket);

	
	//Incoming messages
	void partialFill(Order order);
	void fullyFilled(Order order);
	void cancelled(Order order);
	
	void messageHandler();
}