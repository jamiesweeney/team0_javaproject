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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class OrderManager {


    private static LiveMarketData liveMarketData;
    // Instance variables
    private Logger logger = Logger.getLogger(OrderManager.class);
    protected HashMap<Integer, Order> orders = new HashMap<Integer, Order>(); //debugger will do this line as it gives state to the object
    //currently recording the number of new order messages we get. TODO why? use it for more?
    private int id = 0; //debugger will do this line as it gives state to the object

  
    private boolean isRunning;

    protected Socket[] orderRouters;
    protected List<SocketChannel> clients = new CopyOnWriteArrayList<SocketChannel>();
    protected Socket trader;
    ServerSocketChannel omOpenPort = null;

    protected Queue<OrderJob> clientQueue;



    // Constructor
    public OrderManager(InetSocketAddress[] orderRouters,
                        InetSocketAddress trader,
                        LiveMarketData liveMarketData) {

        System.out.println("Entered Constructor in OrderManager");
        PropertyConfigurator.configure("resources/log4j.properties");
        OrderManager.liveMarketData = liveMarketData;

        try
        {
            omOpenPort = ServerSocketChannel.open();
            omOpenPort.socket().bind(new InetSocketAddress(2025));
            logger.info("Server port opened on port 2025 successfully.");
        }
        catch(IOException e)
        {
            logger.error("IOException caught in OrderManager constructor");
            e.printStackTrace();
        }


        // Set up the order manager
        setup(orderRouters, trader);

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

    private void setup(InetSocketAddress[] orderRouters, InetSocketAddress trader) {
        System.out.println("Entered Setup in OrderManager");



        // Set up trader connection
        this.trader = connect(trader);

        // Fill order routers with connections
        int i = 0;
        this.orderRouters = new Socket[orderRouters.length];
        for (InetSocketAddress location : orderRouters) {
            this.orderRouters[i++] = connect(location);
        }

//        // Fill clients with connections
//        i = 0;
//        this.clients = new Socket[clients.length];
//        for (InetSocketAddress location : clients) {
//            this.clients[i++] = connect(location);
//        }
//
//        // Set up incoming messages queues
        clientQueue = new LinkedBlockingQueue<>();
//
//      // Initiate our message handling threads
        Thread t = new Thread(new ClientWorkerThread(clientQueue ,this));
        t.start();

        Thread th = new Thread(new ClientConnectionThread(this));
        th.start();
        System.out.println("Leaving Setup in OrderMananger");
    }


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
//        logger.error("s is equal to ")
        return s;
    }

    /*
        Contains the main logic for the order manager

        - Checks client messages
        - Checks router messages
        - Checks trader messages
     */
    private void mainLogic() {


        // Constantly check for messages

        while (isRunning)
        {
            // Check each client / router / trader in turn
            if(!clients.isEmpty())
            {
//                System.out.println("In mainLogic, clients is not empty.");
                checkClients();
            }
            if(orderRouters.length != 0)
            {
//                System.out.println("In mainLogic, orderRouters is not empty.");
                checkRouters();
            }
            if(trader.isConnected())
            {
//                System.out.println("In mainLogic, trader is connected.");
                checkTrader();
            }
        }
    }

    /*
        Checks the messages for all clients
        Creates a new order if order requests received.
    */
    private void checkClients()
    {
        System.out.println("Entered check clients in OrderManager.");
        //Socket client;
        ObjectInputStream is;
        ArrayList<Object> args;
        OrderJob job;


        // Iterating over each client
        for(SocketChannel client: clients) {
           System.out.println(client.toString() + "is being processed by the for loop.");

            try {
                // Check if there is any new data
                if (0 < client.socket().getInputStream().available()) {
                    System.out.println("OrderManager:checkClients(), client input stream available.");

                    is = new ObjectInputStream(client.socket().getInputStream()); //create an object inputstream, this is a pretty stupid way of doing it, why not create it once rather than every time around the loop
                    String method = (String) is.readObject();

                    switch (method) {
                        // If a new order single, we want to create a new Order object
                        case "newOrderSingle":
                            args = new ArrayList<>();
                            args.add(clients.indexOf(client));
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


    public synchronized int addOrder(int clientId, int clientOrderId, NewOrderSingle nos){

        // Create the new order and add to the order array
        int new_id = id++;
        Order order = new Order(clientId, clientOrderId, nos.instrument, nos.size, nos.side);
        orders.put(new_id, order);
        return new_id;

    }


    /*
        Sends a new order to the trader through an output stream
    */
    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {

        // Write the order date to the trader stream
        ObjectOutputStream ost = new ObjectOutputStream(trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

    /*
        Checks the messages for all routers
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

                            Order slice = orders.get(orderId).slices.get(sliceId);
                            slice.bestPrices[routerId] = is.readDouble();
                            slice.bestPriceCount += 1;

                            if (slice.bestPriceCount == slice.bestPrices.length)
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

    /*
        Sends a order slice to the router and best price
    */
    private void reallyRouteOrder(int sliceId, Order o) throws IOException {


        // Iterate over prices and find minimum
        // Route to the minimum
        if (o.side == 1)//if buying the stock
        {
            o.routerID = findPurchaseRoute(o);
        }
        else if(o.side == 2)//if selling the stock
        {
            o.routerID = findSalesRoute(o);
        }
        ObjectOutputStream os = new ObjectOutputStream(orderRouters[o.routerID].getOutputStream());
        os.writeObject(Router.api.routeOrder);
        os.writeInt((int) o.id);
        os.writeInt(sliceId);
        os.writeInt((int) o.sizeRemaining());
        os.writeObject(o.instrument);
        os.flush();
    }

    /*
        Creates a new fill order for a order slice
    */
    private void newFill(int id, int sliceId, int size, double price) throws IOException {
        Order o = orders.get(id);
        o.slices.get(sliceId).createFill(size, price);

        // If there is nothing left write to database
        if (o.sizeRemaining() == 0) {
            Database.write(o);
        }
        sendOrderToTrader(id, o, TradeScreen.api.fill);
    }

    /*
        Checks the messages for the trader
    */
    private void checkTrader() {
        ObjectInputStream is;

        try {
            // If there is any messages from the trader
            if (0 < this.trader.getInputStream().available())
            {
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

    /*
        If the trader accepts the new order, prices the order
    */
    private void acceptOrder(int id) throws IOException {
        Order o = orders.get(id);

        // If the order is pending new, order has already been accepted
        if (o.OrdStatus != 'A') {
            logger.error("Error accepting order that has already been accepted");
            return;
        }

        // If not then the order must be new
        o.OrdStatus = '0';
        ObjectOutputStream os = new ObjectOutputStream(clients.get((int)o.clientid).socket().getOutputStream());

        // Write acknowledgement to the client
        generateMessage(os, (int)o.clientOrderID, 'A', 'D', o.side);
        os.flush();

        // price the order
        price(id, o);
    }

    /*
        Updates the live market data
    */
    private void price(int id, Order o) throws IOException {

        //TODO - Why do we send back to the trader with the api.price?
        // Set the market price and send the order to the trader
        liveMarketData.setPrice(o);
        sendOrderToTrader(id, o, TradeScreen.api.price);
    }

    /*
        If the trader requested a slice for the new order
    */
    private void sliceOrder(int id, int sliceSize) throws IOException {
        System.err.println("Entered sliceOrder() in OrderManager");

        Order o = orders.get(id);

        //Order has a list of slices, and a list of fills, each slice is a child order and each fill is associated with either a child order or the original order
        //Make sure that the slice size is valid (must be less than the remaining orders)
        if (sliceSize > o.sizeRemaining() - o.sliceSizes()) {
            logger.error("error sliceSize is bigger than remaining size to be filled on the order");
            return;
        }

        // If valid slice, create a new slice
        int sliceId = o.newSlice(sliceSize);
        Order slice = o.slices.get(sliceId);

        // Do internal cross with slice
        internalCross(id, slice);
        int sizeRemaining = (int) o.slices.get(sliceId).sizeRemaining();

        // If the internal cross does not satisfy then route to exchange
        if (sizeRemaining > 0) {
            routeOrder(id, sliceId, sizeRemaining, slice);
            o.routeCode = 2;
        }
    }


    /*
        Performs an internal cross, if theres 2 matching buy/sell then match them

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
            } else if (!(matchingOrder.instrument.equals(o.instrument))) {
                continue;

                // Don't include non matching prices
            } else if (!(matchingOrder.initialMarketPrice == o.initialMarketPrice)) {
                continue;

                // Don't include orders with same side
            } else if ((matchingOrder.side == o.side)) {
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

    /*
    routeOrder basically just sends the order to the exchanges and get a price for them
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
            os.writeObject(order.instrument);
            os.writeInt((int) order.sizeRemaining());
            os.flush();
        }

        // need to wait for these prices to come back before routing
        order.bestPrices = new double[orderRouters.length];
        order.bestPriceCount = 0;
    }


    private void cancelOrder(int orderID) {
        Order o = orders.get(orderID);
        try {
            ObjectOutputStream os = new ObjectOutputStream(clients.get((int) o.clientid).socket().getOutputStream());

            if (o.OrdStatus == '0')
            {
                orders.remove(orderID);
                generateMessage(os, (int)o.clientOrderID, '4','F',o.side);
                os.flush();
            }
            else if (o.OrdStatus == '2') {
                generateMessage(os, (int)o.clientOrderID, '8', '9', o.side);
                os.flush();
            } else {
                int rmvdContent = 0;
                int filledCount = 0;
                for (Order slice : o.slices) {
                    if (slice.OrdStatus == '2') {
                        filledCount++;
                    } else if (slice.OrdStatus == '0') {
                        o.slices.remove(slice);
                        rmvdContent++;
                    }
                }
                generateMessage(os, (int)o.clientOrderID, '4', 'F', o.side);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendCancel(Order order, Socket routerSocket) {
//        orderRouter.sendCancel(order);
//        order.orderRouter.writeObject(order);
        try {
            ObjectOutputStream os;
            os = new ObjectOutputStream(routerSocket.getOutputStream());
            for (int i = 0; i < order.slices.size(); i++) {
                os.writeObject(Router.api.sendCancel);
                os.writeInt((int)order.id);
                os.writeInt(i);
                os.writeObject(order.instrument);
                //os.writeInt((int) order.sizeRemaining());
                os.flush();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void cancelSuccess(int orderID, int sliceID)
    {
        Order o = orders.get(orderID);
        try
        {
            ObjectOutputStream os = new ObjectOutputStream(clients.get((int) o.clientid).socket().getOutputStream());
            generateMessage(os, (int)o.clientOrderID, '4', '9', o.side);
            os.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void generateMessage(ObjectOutputStream os, int clientOID, char ordStatus, char msgType, int side)
    {
        try {
            os.writeObject("11=" + clientOID + ";39=" + ordStatus + ";35=" + msgType + "54=" + side);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private int findPurchaseRoute(Order o)
    {

        int minIndex = 0;
        double min = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (min > o.bestPrices[i]) {
                minIndex = i;
                min = o.bestPrices[i];
            }
        }
        return minIndex;
    }
    private int findSalesRoute(Order o)
    {
        int maxIndex = 0;
        double max = o.bestPrices[0];
        for (int i = 1; i < o.bestPrices.length; i++) {
            if (max < o.bestPrices[i]) {
                maxIndex = i;
                max = o.bestPrices[i];
            }
        }
        return maxIndex;
    }

    public void startOM()
    {
        isRunning = true;
    }
    public void stopOM()
    {
        isRunning = false;
    }
}

