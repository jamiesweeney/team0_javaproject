package OrderClient;

import java.io.IOException;

import OrderManager.Order;
import Ref.Instrument;

public interface Client{
	//Outgoing messages
	int sendOrder(int id, int size,char msgType, float price, Instrument ins, int side)throws IOException;
	void sendCancel(int id);
	
	//Incoming messages
	void partialFill(Order order);
	void fullyFilled(Order order);
	void cancelled(Order order);
	
	void messageHandler();
}