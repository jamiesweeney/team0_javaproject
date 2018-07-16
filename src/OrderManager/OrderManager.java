package OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;

import Database.Database;
import LiveMarketData.LiveMarketData;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import OrderRouter.Router.api;
import TradeScreen.TradeScreen;

public class OrderManager {


    // Instance variables


	private HashMap<Integer,Order> orders=new HashMap<Integer,Order>(); //debugger will do this line as it gives state to the object
	//currently recording the number of new order messages we get. TODO why? use it for more?
    private int id=0; //debugger will do this line as it gives state to the object

    private static LiveMarketData liveMarketData;
    private Socket[] orderRouters;
	private Socket[] clients;
	private Socket trader;


	//TODO - fix this
	private Socket connect(InetSocketAddress location) throws InterruptedException{
		boolean connected=false;
		int tryCounter=0;
		while(!connected&&tryCounter<600){
			try{
				Socket s=new Socket(location.getHostName(),location.getPort());
				s.setKeepAlive(true);
				return s;
			}catch (IOException e) {
				Thread.sleep(1000);
				tryCounter++;
			}
		}
		System.out.println("Failed to connect to "+location.toString());
		return null;
	}


	//@param args the command line arguments
    // Contains the main order manager logic
	public OrderManager(InetSocketAddress[] orderRouters, InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData)throws InterruptedException{

	    // Set up the order manager
        setup(orderRouters, clients, trader, liveMarketData);

		// Start doing the main logic
		mainLogic();
	}


	private void setup(InetSocketAddress[] orderRouters, InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData) throws InterruptedException {
        // Set up instance variables
        this.liveMarketData=liveMarketData;
        this.trader=connect(trader);
        this.orderRouters=new Socket[orderRouters.length];
        this.clients=new Socket[clients.length];

        // Fill order routers with connections
        int i=0;
        for(InetSocketAddress location:orderRouters){
            this.orderRouters[i++]=connect(location);
            i++;
        }

        // Fill clients with connections
        i=0;
        for(InetSocketAddress location:clients){
            this.clients[i++]=connect(location);
        }
    }


    /*
        Contains the main logic for the order manager

        - Checks client messages
        - Checks router messages
        - Checks trader messages
     */
	private void mainLogic(){

        // Constantly check for messages
        while(true){

            // Check each client / router / trader in turn
            checkClients();
            checkRouters();
            checkTrader();
        }
    }


    /*
        Checks the messages for all clients
        Creates a new order if order requests received.
     */
    private void checkClients(){
	    int clientId;
	    Socket client;
        ObjectInputStream is;

        // Iterating over each client
        for(clientId=0;clientId<this.clients.length;clientId++){
            client=this.clients[clientId];

            try {
                // Check if there is any new data
                if(0<client.getInputStream().available()){

                    is = new ObjectInputStream(client.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method=(String)is.readObject();
                    System.out.println(Thread.currentThread().getName()+" calling "+method);

                    // Determine the message type
                    switch(method){

                        // If a new order single, we want to create a new Order object
                        case "newOrderSingle":
                            newOrder(clientId, is.readInt(), (NewOrderSingle)is.readObject());
                            break;
                        //TODO create a default case which errors with "Unknown message type"+...
                    }
                }
                // TODO - handle properly
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    /*
        Checks the messages for all routers



     */
    private void checkRouters(){
        int routerId;
        Socket router;
        ObjectInputStream is;


        // Iterating over each router
        for(routerId=0;routerId<this.orderRouters.length;routerId++){ //check if we have data on any of the sockets
            router=this.orderRouters[routerId];

            try {
                // Check if there is any new data
                if(0<router.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages

                    is = new ObjectInputStream(router.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) is.readObject();
                    System.out.println(Thread.currentThread().getName() + " calling " + method);

                    // Determine the message type
                    switch (method) {

                        //TODO - Figure out what is happening here
                        // If a best price message, we want to
                        case "bestPrice":
                            int OrderId = is.readInt();
                            int SliceId = is.readInt();

                            Order slice = orders.get(OrderId).slices.get(SliceId);
                            slice.bestPrices[routerId] = is.readDouble();
                            slice.bestPriceCount += 1;

                            if (slice.bestPriceCount == slice.bestPrices.length)
                                reallyRouteOrder(SliceId, slice);
                            break;

                        //TODO - Figure out what is happening here
                        // If a new fill order, we want to
                        case "newFill":
                            newFill(is.readInt(), is.readInt(), is.readInt(), is.readDouble());
                            break;
                    }
                }
                // TODO - handle properly
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


    }

    /*
        Checks the messages for the trader



     */
    private void checkTrader(){
        ObjectInputStream is;

        try {
            // If there is any messages from the trader
            if(0<this.trader.getInputStream().available()){
                is=new ObjectInputStream(this.trader.getInputStream());
                String method=(String)is.readObject();
                System.out.println(Thread.currentThread().getName()+" calling "+method);

                // Determine the message type
                switch(method){

                    // If the trader has accepted the new order
                    case "acceptOrder":
                        acceptOrder(is.readInt());
                        break;

                    // If the trader has sliced the order
                    case "sliceOrder":
                        sliceOrder(is.readInt(), is.readInt());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



    // Client request logic
    /*
        Deals with new order requests from the client
     */
    private void newOrder(int clientId, int clientOrderId, NewOrderSingle nos) throws IOException{


        // Create the new order and add to the order array
        Order order = new Order(clientId, clientOrderId, nos.instrument, nos.size);
        orders.put(id,order);

        // Send a message to the client with 39=A;
        // OrdStatus is Fix 39, 'A' is 'Pending New'
        ObjectOutputStream os=new ObjectOutputStream(clients[clientId].getOutputStream());
        os.writeObject("11="+clientOrderId+";35=A;39=A;");
        os.flush();

        // Send this order to the trading screen
        sendOrderToTrader(id,orders.get(id),TradeScreen.api.newOrder);

        id++;
    }

    /*
        Sends a new order to the trader
     */
    private void sendOrderToTrader(int id,Order o,Object method) throws IOException{

        // Write the order date to the trader stream
        ObjectOutputStream ost=new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }


    //Router request logic
    /*

     */
    private void reallyRouteOrder(int sliceId,Order o) throws IOException{

        //TODO this assumes we are buying rather than selling
        int minIndex=0;
        double min=o.bestPrices[0];
        for(int i=1;i<o.bestPrices.length;i++){
            if(min>o.bestPrices[i]){
                minIndex=i;
                min=o.bestPrices[i];
            }
        }


        ObjectOutputStream os=new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
        os.writeObject(Router.api.routeOrder);
        os.writeInt(o.id);
        os.writeInt(sliceId);
        os.writeInt(o.sizeRemaining());
        os.writeObject(o.instrument);
        os.flush();
    }
    private void newFill(int id,int sliceId,int size,double price) throws IOException{
        Order o=orders.get(id);
        o.slices.get(sliceId).createFill(size, price);
        if(o.sizeRemaining()==0){
            Database.write(o);
        }
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }


    // Trader request logic
    /*
        If the trader accepts the new order
     */
	public void acceptOrder(int id) throws IOException{
		Order o=orders.get(id);

		// If the order is pending new, order has already been accepted
		if(o.OrdStatus!='A'){
			System.out.println("error accepting order that has already been accepted");
			return;
		}

		// If not then the order must be new
		o.OrdStatus='0';
		ObjectOutputStream os=new ObjectOutputStream(clients[o.clientid].getOutputStream());

		// Write acknowledgement to the client
		os.writeObject("11="+o.ClientOrderID+";35=A;39=0");
		os.flush();

		// price the order
		price(id,o);
	}

    private void price(int id,Order o) throws IOException{

	    // Set the market price and send the order to the trader
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }

    /*
        If the trader requested a slice for the new order
     */
	public void sliceOrder(int id,int sliceSize) throws IOException{
		Order o=orders.get(id);


		//Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
		//Make sure that the slice size is valid (must be less than the remaining orders)
        if(sliceSize>o.sizeRemaining()-o.sliceSizes()){
			System.out.println("error sliceSize is bigger than remaining size to be filled on the order");
			return;
		}

		// If valid slice, slice the order
		int sliceId=o.newSlice(sliceSize);
		Order slice=o.slices.get(sliceId);

		internalCross(id,slice);
		int sizeRemaining=o.slices.get(sliceId).sizeRemaining();
		if(sizeRemaining>0){
			routeOrder(id,sliceId,sizeRemaining,slice);
		}
	}

    private void internalCross(int id, Order o) throws IOException{


	    // Iterating over all the orders
        for(Map.Entry<Integer, Order>entry:orders.entrySet()){

            // Skip the parent order
            if(entry.getKey().intValue()==id){
                continue;
            }

            // Skip orders with same instrument and market price
            Order matchingOrder=entry.getValue();
            if(!(matchingOrder.instrument.equals(o.instrument)&&matchingOrder.initialMarketPrice==o.initialMarketPrice)){
                continue;
            }

            //TODO add support here and in Order for limit orders
            int sizeBefore=o.sizeRemaining();
            o.cross(matchingOrder);

            // If size has changed, send the order to the trader
            if(sizeBefore!=o.sizeRemaining()){
                sendOrderToTrader(id, o, TradeScreen.api.cross);
            }
        }
    }




    // Router request logic
    private void routeOrder(int id,int sliceId,int size,Order order) throws IOException{
        ObjectOutputStream os;

	    // Iterate over router sockets
        for(Socket r:orderRouters){
            os=new ObjectOutputStream(r.getOutputStream());

            // Send the order details
            os.writeObject(Router.api.priceAtSize);
            os.writeInt(id);
            os.writeInt(sliceId);
            os.writeObject(order.instrument);
            os.writeInt(order.sizeRemaining());
            os.flush();
        }

        // need to wait for these prices to come back before routing
        order.bestPrices=new double[orderRouters.length];
        order.bestPriceCount=0;
    }


	private void cancelOrder(){
		
	}


	private void sendCancel(Order order,Router orderRouter){
		//orderRouter.sendCancel(order);
		//order.orderRouter.writeObject(order);
	}

}