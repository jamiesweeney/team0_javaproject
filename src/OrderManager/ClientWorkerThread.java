package OrderManager;

import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import TradeScreen.TradeScreen;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;


/** Runs in the background, takes in incoming messages from the client message queue
 *  and deals with them.
 *
 */
public class ClientWorkerThread implements Runnable{

    private Logger logger = Logger.getLogger(OrderManager.class);
    Queue<OrderJob> inputQ;
    OrderManager om;

    /** Constructor
     *
     * @param inputQ the queue that holds the incoming orders, shared between threads
     * @param om the order manager, used to access OM variables
     */
    ClientWorkerThread(Queue inputQ,OrderManager om){
        this.inputQ = inputQ;
        this.om=om;
    }


    /** Runs the worker thread process
     * Constantly checks for new messages in the queue and deals with them
     *
     */
    @Override
    public void run() {

        OrderJob job;
        int id;
        int clientId;
        int clientOrderId;
        NewOrderSingle nos;

        // Constantly checks for new messages
        while (true){
            if ((job = inputQ.poll()) != null){

                //
                switch (job.method) {
                        // If a new order single, we want to create a new Order object
                        case "newOrderSingle":

                            // Get info from job
                            clientId = (int)job.args.get(0);
                            clientOrderId = (int) job.args.get(1);
                            nos = (NewOrderSingle) job.args.get(2);

                            // Add the order ot OM
                            id = om.addOrder(clientId, clientOrderId, nos);

                            // Send a message to the client and to the trader
                            ObjectOutputStream os = null;
                            try {
                                os = new ObjectOutputStream(om.clients[clientId].getOutputStream());

                                generateMessage(os, clientOrderId, 'A', 'D', nos.side);
                                os.flush();

                                sendOrderToTrader(id, om.orders.get(id), TradeScreen.api.newOrder);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            break;

                        // If a send cancel message type
                        case "sendCancel":
                            id = (int)job.args.get(0);

                            // Get the order type
                            Order o = om.orders.get(id);

                            // cancel if the order has not been routed
                            if (o.getRouteCode() == 0) {
                                cancelOrder(id);
                            }

                            // send a cancel request to the router if the order has been routed
                            if (o.getRouteCode() == 2) {
                                sendCancel(o, om.orderRouters[o.getRouterID()]);
                            }
                            break;


                        default:
                            logger.error("Error, unknown mesage type: " + job.method);
                            break;
                }
            }
        }
    }


    /** Taken from the OM, simply generates a message and passes it through the output stream
     *
     * @param os the output stream where we want to send the message
     * @param clientOID the client order id, the unique id for the client order
     * @param ordStatus the order status, tag number 39
     * @param msgType the message type, tag number 39
     * @param side the side, 1 for buy 2 for sell
     */
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

    /** Forwards a new order to the trader for decision.
     *
     * @param id the OM order id
     * @param o the order object itself
     * @param method the type of message we are sending
     * @throws IOException
     */
    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {

        // Write the order date to the trader stream
        ObjectOutputStream ost = new ObjectOutputStream(om.trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

    /** Handles a cancel order message from the client
     *
     * @param orderID
     */


    private void cancelOrder(int orderID) {
        Order o = om.orders.get(orderID);
        try {
            ObjectOutputStream os = new ObjectOutputStream(om.clients[(int) o.getClientID()].getOutputStream());

            // If the order has been fufilled, send rejected cancel message
            if (o.getOrdStatus() == '2') {
                generateMessage(os, (int)o.getClientOrderID(), '8', '9', o.getSide());
                os.flush();

            // If not then cancel each slice in turn
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

                // Send back a cancelled message
                generateMessage(os, (int)o.getClientOrderID(), '4', '9', o.getSide());
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** Attempts to cancel a routed order by requesting the router
     * to cancel it.
     *
     * @param order the order we wish to cancel
     * @param routerSocket the socket that the router is connected to
     */
    private void sendCancel(Order order, Socket routerSocket) {
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
}
