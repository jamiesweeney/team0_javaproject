package OrderManager;

import Database.Database;
import LiveMarketData.LiveMarketData;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import TradeScreen.TradeScreen;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <h1>OrderManager</h1>
 * */
public class OrderManager {


    private static LiveMarketData liveMarketData;
    // Instance variables
    private Logger logger = Logger.getLogger(OrderManager.class);
    protected HashMap<Integer, Order> orders = new HashMap<Integer, Order>(); //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int id = 0; //debugger will do this line as it gives state to the object

  
    private boolean isRunning;

    protected Socket[] orderRouters;
    protected Socket[] clients;
    protected Socket trader;


    protected Queue<OrderJob> clientQueue;



    /**
     * OrderManager is the reworked constructor of the OrderManager class.
     * It takes several arguments which are used to initialise it's member variables.
     * */
    // Constructor
    public OrderManager(InetSocketAddress[] orderRouters,
                        InetSocketAddress[] clients,
                        InetSocketAddress trader,
                        LiveMarketData liveMarketData)
    {
        PropertyConfigurator.configure("resources/log4j.properties");
        OrderManager.liveMarketData = liveMarketData;

        // Set up the order manager
        setup(orderRouters, clients, trader);


        startOM();


        // Start doing the main logic
        mainLogic();
    }

    //Constructor used to make server take connections and not seek them.
    //Default server port set to 2025.
    public OrderManager()
    {
        try
        {
            Socket omOpenPort = new ServerSocket(2025).accept();
            logger.info("Server port opened on port 2025 successfully.");
        }
        catch(IOException e)
        {
            logger.error("IOException caught in OrderManager constructor");
            e.printStackTrace();
        }
    }

    /**
     * setup() is called from the constructor and initialises the trader and routers variables.
     * This method connects the program through Sockets, which are used to transfer the data.
     * */
    private void setup(InetSocketAddress[] orderRouters, InetSocketAddress[] clients, InetSocketAddress trader) {

        // Set up trader connection
        this.trader = connect(trader);


        // Fill order routers with connections
        int i = 0;
        this.orderRouters = new Socket[orderRouters.length];
        for (InetSocketAddress location : orderRouters) {
            this.orderRouters[i++] = connect(location);
        }


        // Fill clients with connections
        i = 0;
        this.clients = new Socket[clients.length];
        for (InetSocketAddress location : clients) {
            this.clients[i++] = connect(location);
        }


        // Set up incoming messages queues
        clientQueue = new LinkedBlockingQueue<>();

        // Initiate our message handling threads
        Thread t = new Thread(new ClientWorkerThread(clientQueue ,this));
        t.start();
    }


    /**
     *connect() takes a Socket as an argument and is used to connect the OrderManager object to the socket it is passed.
     * The method will attempt a connection 600 times before throwing an error.
     * */
    // Creates a socket to an address
    private Socket connect(InetSocketAddress location) {
        int tryCounter = 0;
        Socket s = null;

        // Try and connect 600 times
        while (tryCounter < 600) {
            try {
                // Create the socket
                s = new Socket(location.getHostName(), location.getPort());
                s.setKeepAlive(true);
                return s;
            } catch (IOException e) {
                tryCounter++;
            }
        }
        logger.error("Failed to connect to " + location.toString());
        return s;
    }

    /*
        Contains the main logic for the order manager

        - Checks client messages
        - Checks router messages
        - Checks trader messages
     */

    /**
     * The mainLogic() method contains the core functionality of the object.
     * It contains a while() loop which will iterate so long as the isRunning variable is true.
     * Each iteration of the while() loop, the program will check for any inbound messages from the clients, routers and trader.
     * If data is received, it delegates the manipulation of the data to the appropriate method.
     * */
    private void mainLogic() {
//        System.out.println("sajsa");

        // Constantly check for messages

        while (isRunning) {


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
    /**
     * checkClients() is called from within the mainLogic() while() loop.
     * Its objective is to check for any inbound messages from the clients that are connected to the OrderManager object, and creates a new order if
     * it receives a request for one.
     * */
    private void checkClients() {



        int clientId;
        Socket client;
        ObjectInputStream is;
        ArrayList<Object> args;
        OrderJob job;
//        System.out.println("in checkCLients");


        // Iterating over each client
        for (clientId = 0; clientId < this.clients.length; clientId++) {
            client = this.clients[clientId];

//            System.out.println("Going through clients");

            try {
                // Check if there is any new data
                if (0 < client.getInputStream().available()) {

                    System.out.println("Got a message");

                    is = new ObjectInputStream(client.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) is.readObject();

                    switch (method) {
                        // If a new order single, we want to create a new Order object
                        case "newOrderSingle":
                            args = new ArrayList<>();
                            args.add(clientId);
                            args.add(is.readInt());
                            args.add(is.readObject());

                            job = new OrderJob(method, args);

                            clientQueue.add(job);
                            System.out.println(job.toString());
                            break;

                        case "sendCancel":

                            args = new ArrayList<>();
                            args.add(is.readInt());

                            job = new OrderJob(method, args);

                            clientQueue.add(job);

                            break;

                        default:
                            logger.error("Error, unknown message type: " + method);
                            break;
                    }
                }
            } catch (IOException e) {
                // TODO - TEAM 15
                logger.error("IOException detected: " + e);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO - TEAM 15
                logger.error("ClassNotFoundException detected: " + e);
                e.printStackTrace();
            }


        }
    }


    /**
     * addOrder() is used to add the order it is passed to the order collection object: orders.
     * It adds extra indexing data: the id of the client, and the id of the clients order.
     * */
    public synchronized int addOrder(int clientId, int clientOrderId, NewOrderSingle nos){

        // Create the new order and add to the order array
        int new_id = id++;
        Order order = new Order(clientId, clientOrderId, nos.instrument, nos.size, nos.side);
        orders.put(new_id, order);
        return new_id;

    }



    /**
     * sendOrderToTrader() is used to pass new orders to the trader using an OutputStream.
     * This function also throws an IOException.
     * */
    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {

        // Write the order date to the trader stream
        ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

    /**
     * checkRouters() is used to check for any messages from all the routers contained in orderRouters.
     */
    private void checkRouters() {
        int routerId;
        Socket router;
        ObjectInputStream is;


        // Iterating over each router
        for (routerId = 0; routerId < this.orderRouters.length; routerId++) {
            router = this.orderRouters[routerId];

            try {
                // Check if there is any new data
                if (0 < router.getInputStream().available()) { //if we have part of a message ready to read, assuming this doesn't fragment messages

                    is = new ObjectInputStream(router.getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) is.readObject();
                    logger.info(Thread.currentThread().getName() + " calling " + method);

                    // Determine the message type
                    switch (method) {
                        //TODO - Figure out what is happening here
                        // If a best price message, we want to
                        case "bestPrice":
                            int orderId = is.readInt();
                            int sliceId = is.readInt();

                            Order slice = orders.get(orderId).getSlices().get(sliceId);
                            slice.getBestPrices()[routerId] = is.readDouble();
                            //slice.bestPriceCount += 1;
                            slice.setBestPriceCount(slice.getBestPriceCount() + 1);
                            if (slice.getBestPriceCount() == slice.getBestPrices().length)
                                reallyRouteOrder(sliceId, slice);
                            break;
                        case "orderCancelled":
                            orderId = is.readInt();
                            sliceId = is.readInt();
                            cancelSuccess(orderId, sliceId);
                            break;

                        //TODO - Figure out what is happening here
                        // If a new fill order, we want to
                        case "newFill":
                            newFill(is.readInt(), is.readInt(), is.readInt(), is.readDouble());
                            break;
                    }
                }

            } catch (IOException e) {
                // TODO - TEAM 15
                logger.error("IOException detected: " + e);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO - TEAM 15
                logger.error("ClassNotFoundException detected: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     reallyRouteOrder() takes 2 arguments: a slice id and an order.
     The method then determines whether the order is a buy or a sell, then sends the order to the exchange.
    */
    private void reallyRouteOrder(int sliceId, Order o) throws IOException {

        // Iterate over prices and find minimum
        // Route to the minimum
        if (o.getSide() == 1)//if buying the stock
        {
            o.setRouterID(findPurchaseRoute(o));
        }
        else if(o.getSide() == 2)//if selling the stock
        {
            o.setRouterID(findSalesRoute(o));
        }
        ObjectOutputStream os = new ObjectOutputStream(orderRouters[o.getRouterID].getOutputStream());
        os.writeObject(Router.api.routeOrder);
        os.writeInt((int) o.getOmID());
        os.writeInt(sliceId);
        os.writeInt((int) o.sizeRemaining());
        os.writeObject(o.getInstrument());
        os.flush();
    }


    /**
     * newFill() takes arguments for an order id, a slice id, a n order size and a price.
     * The method then creates a new fill order for an order slice, and passes the order to the trader as a fill.
     */
    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order o = orders.get(id);
        o.getSlices().get(sliceId).createFill(size, price);

        // If there is nothing left write to database
        if (o.sizeRemaining() == 0) {
            Database.write(o);
        }
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }


    /**
     * checkTrader() is used to check for any messages for the trader.
     * If there are messages to process, process them by type, i.e acceptOrder, sliceOrder etc...
     */
    private void checkTrader() {
        ObjectInputStream is;

        try {
            // If there is any messages from the trader
            if (0 < this.trader.getInputStream().available()) {
                is = new ObjectInputStream(this.trader.getInputStream());
                String method = (String) is.readObject();
                logger.info(Thread.currentThread().getName() + " calling " + method);
                // Determine the message type
                switch (method) {
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
            // TODO - TEAM 15
            logger.error("IOException detected: " + e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO - TEAM 15
            logger.error("ClassNotFoundException detected: " + e);
            e.printStackTrace();
        }
    }

    /**
     * acceptOrder() takes an orderid as an int, and pulls the order from the orders container.
     * If the order has not already been accepted, the method will generate an output stream and price the order accordingly.
     * */
    private void acceptOrder(int id) throws IOException {
        Order o = orders.get(id);

        // If the order is pending new, order has already been accepted
        if (o.getOrdStatus() != 'A') {
            logger.error("Error accepting order that has already been accepted");
            return;
        }

        // If not then the order must be new
        o.setOrdStatus('0');
        ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.getClientID()].getOutputStream());

        // Write acknowledgement to the client
        generateMessage(os, (int)o.getClientOrderID, '2', '8', o.getSide);
        os.flush();

        // price the order
        price(id, o);
    }



    /**
     * The price() method is used to update the live market data. Once the market data is updated, the order is sent to the trader.*
     */
    private void price(int id, Order o) throws IOException {

        //TODO - Why do we send back to the trader with the api.price?
        // Set the market price and send the order to the trader
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }

    /**
     sliceOrder() is called if a trader needs to slice an order. The method takes an id and a slice size
     which it uses to locate the order to slice it to the desired size.
    */
    private void sliceOrder(int id, int sliceSize) throws IOException {

        Order o = orders.get(id);

        //Order has a list of slices, and a list of fills, each slice is a child order and each fill is associated with either a child order or the original order
        //Make sure that the slice size is valid (must be less than the remaining orders)
        if (sliceSize > o.sizeRemaining() - o.sliceSizes()) {
            logger.error("error sliceSize is bigger than remaining size to be filled on the order");
            return;
        }

        // If valid slice, create a new slice
        int sliceId = o.newSlice(sliceSize);
        Order slice = o.getSlices().get(sliceId);

        // Do internal cross with slice
        internalCross(id, slice);
        int sizeRemaining = (int) o.getSlices().get(sliceId).sizeRemaining();

        // If the internal cross does not satisfy then route to exchange
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
            o.setRouteCode(2);
        }
    }


    /**
        internalCross() performs an internal cross. If there's 2 matching buy/sell then match them.

        The internal cross attempts to match 2 trades that can be completed within the OM system
        as opposed to routing it to the exchange. This avoids exchange fees and makes the bank/clients more money
        overall.
     */
    private void internalCross(int id, Order o) throws IOException {

        // Iterating over all the orders
        for (Map.Entry<Integer, Order> entry : orders.entrySet()) {

            Order matchingOrder = entry.getValue();

            // Don't include the order we're trying to cross
            if (entry.getKey() == id) {
                continue;

                // Don't include non equal instruments
            } else if (!(matchingOrder.getInstrument().equals(o.getInstrument()))) {
                continue;

                // Don't include non matching prices
            } else if (!(matchingOrder.getInitialMarketPrice() == o.getInitialMarketPrice())) {
                continue;

                // Don't include orders with same side
            } else if ((matchingOrder.getSide() == o.getSide())) {
                continue;
            }

            //TODO add support here and in Order for limit orders

            // If everything passed, cross the orders
            int sizeBefore = (int) o.sizeRemaining();
            o.cross(matchingOrder);

            // If size has changed, send the order to the trader
            if (sizeBefore != o.sizeRemaining()) {
                sendOrderToTrader(id, o, TradeScreen.api.cross);
            }
        }
    }


    // Router request logic

    /**
    routeOrder() basically just sends the order to the exchanges and get a price for them
    in comparison reallyRouteOrder picks the best price and routes the order to that exchange
    */
    private void routeOrder(int id, int sliceId, int size, Order order) throws IOException {

        ObjectOutputStream os;

        // Iterate over router sockets
        for (Socket r : orderRouters) {
            os = new ObjectOutputStream(r.getOutputStream());

            // Send the order details
            os.writeObject(Router.api.priceAtSize);
            os.writeInt(id);
            os.writeInt(sliceId);
            os.writeObject(order.getInstrument());
            os.writeInt((int) order.sizeRemaining());
            os.flush();
        }

        // need to wait for these prices to come back before routing
        order.setBestPrices(new double[orderRouters.length]);
        order.setBestPriceCount(0);
    }

    /**
     * cancelOrder() takes an order id which references an order in the orders container.
     * This function then removes the order from the orders container and generates a message with specific tags depending on the
     * order status.
     **/
    private void cancelOrder(int orderID) {
        Order o = orders.get(orderID);
        try {
            ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.getClientID()].getOutputStream());
            if (o.OrdStatus == '0')
            {
                orders.remove(orderID);
                generateMessage(os, (int)o.clientOrderID, '4','F',o.getSide());
                os.flush();
            }
            else if (o.OrdStatus == '2') {
                generateMessage(os, (int)o.clientOrderID, '8', '9', o.getSide());
                os.flush();
            } else {
                int rmvdContent = 0;
                int filledCount = 0;
                for (Order slice : o.getSlices()) {
                    if (slice.getOrdStatus() == '2') {
                        filledCount++;
                    } else if (slice.getOrdStatus() == '0') {
                        o.getSlices().remove(slice);
                        rmvdContent++;
                    }
                }
                generateMessage(os, (int)o.getClientOrderID, '2', '9', o.getSide());
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/**
 * sendCancel() takes an order and a socket as arguments, and using an OutputStream pointed at the router socket,
 * writes a cancellation message.
 */
    private void sendCancel(Order order, Socket routerSocket) {
//        orderRouter.sendCancel(order);
//        order.orderRouter.writeObject(order);
        try {
            ObjectOutputStream os;
            os = new ObjectOutputStream(routerSocket.getOutputStream());
            for (int i = 0; i < order.getSlices().size(); i++) {
                os.writeObject(Router.api.sendCancel);
                os.writeInt((int)order.getOmID());
                os.writeInt(i);
                os.writeObject(order.getInstrument());
                //os.writeInt((int) order.sizeRemaining());
                os.flush();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * cancelSuccess() takes both an orderID and a sliceID as arguments and works by getting the requested order from
     * orders and writing a message with 4 as the order status and 9 as the msgType.
     */
    private void cancelSuccess(int orderID, int sliceID)
    {
        Order o = orders.get(orderID);
        try {
            ObjectOutputStream os = new ObjectOutputStream(clients[(int) o.getClientID()].getOutputStream());
            generateMessage(os, (int)o.getClientOrderID(), '4', '9', o.getSide());
            os.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * generateMessage() takes in data as arguments and writes the data to an object in a specific format.
     * */
    private void generateMessage(ObjectOutputStream os, int clientOID, char ordStatus, char msgType, int side)
    {
        try {
            os.writeObject("11=" + clientOID + ";39=" + ordStatus + ";35=" + msgType + ";54=" + side);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * findPurchaseRoute() takes an order as an argument, and works by finding the best
     * available price for the order, by comparing the values in the bestPrices container.
     * */
    private int findPurchaseRoute(Order o)
    {

        int minIndex = 0;
        double minPrice = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (minPrice > o.bestPrices[i]) {
                minIndex = i;
                minPrice = o.bestPrices[i];
            }
        }
        return minIndex;
    }

    /**
     * findSalesRoute takes an order as an argument, and based on the values of bestPrices within the order, will return an
     * int corresponding to the index of the best available price.
     */
    private int findSalesRoute(Order o)
    {
        int maxIndex = 0;
        double maxPrice = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (maxPrice < o.bestPrices[i]) {
                maxIndex = i;
                maxPrice = o.bestPrices[i];
            }
        }
        return maxIndex;
    }


    /**
     * startOM() is used to activate the while() loop within mainLogic().
     * */
    public void startOM()
    {
        isRunning = true;
    }

    /**
     * stopOM() is used to deactivate the while() loop within mainLogic().
     * */
    public void stopOM()
    {
        isRunning = false;
    }
}

