package OrderManager;

import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import TradeScreen.TradeScreen;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import static OrderRouter.Router.api.sendCancel;

public class ClientWorkerThread implements Runnable{

    private Logger logger = Logger.getLogger(OrderManager.class);
    Queue<OrderJob> inputQ;
    OrderManager om;

    ClientWorkerThread(Queue inputQ,OrderManager om){
        this.inputQ = inputQ;
        this.om=om;
        System.out.println("Exiting ClientWorkerThread constructor.");
    }

    @Override
    public void run() {


        System.out.println("Entered ClientWorkerThread run() method.");
        OrderJob job;

        int id;

        while (true){
            if ((job = inputQ.poll()) != null){
                System.out.println("Worker has a job to do");

                switch (job.method) {
                        // If a new order single, we want to create a new Order object
                        case "newOrderSingle":

                            int clientId = (int)job.args.get(0);
                            int clientOrderId = (int) job.args.get(1);
                            NewOrderSingle nos = (NewOrderSingle) job.args.get(2);

                            id = om.addOrder(clientId, clientOrderId, nos);

                            // Send a message to the client
                            ObjectOutputStream os = null;
                            try {
                                os = new ObjectOutputStream(om.clients.get(clientId).getOutputStream());

                                generateMessage(os, clientOrderId, 'A', 'D', nos.side);
                                os.flush();

                                sendOrderToTrader(id, om.orders.get(id), TradeScreen.api.newOrder);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }



                            // Send this order to the trading screen

                            break;
                        case "sendCancel":
                            id = (int)job.args.get(0);

                            Order o = om.orders.get(id);
                            if (o.routeCode == 0) {
                                cancelOrder(id);
                            }
                            if (o.routeCode == 2) {
                                sendCancel(o, om.orderRouters[o.routerID]);
                            }
                            break;
                        //TODO create a default case which errors with "Unknown message type"+...
                        default:
                            logger.error("Error, unknown message type: " + job.method);
                            break;
                }
            }
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


    private void sendOrderToTrader(int id, Order o, Object method) throws IOException {

        // Write the order date to the trader stream
        ObjectOutputStream ost = new ObjectOutputStream(om.trader.getOutputStream());
        ost.writeObject(method);
        ost.writeInt(id);
        ost.writeObject(o);
        ost.flush();
    }

    //TODO - implement this
    private void cancelOrder(int orderID) {
        Order o = om.orders.get(orderID);
        try {
            ObjectOutputStream os = new ObjectOutputStream(om.clients.get((int) o.clientid).getOutputStream());


            if (o.OrdStatus == '2') {
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
                generateMessage(os, (int)o.clientOrderID, '4', '9', o.side);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendCancel(Order order, Socket routerSocket) {
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

}
