package OrderManager;

import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import TradeScreen.TradeScreen;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class ClientConnectionThread implements Runnable{

    private Logger logger = Logger.getLogger(OrderManager.class);
    OrderManager om;

    ClientConnectionThread(OrderManager om){
        this.om = om;
    }

    @Override
    public void run() {

        while (true){

            try{
                System.out.println("Waiting for a client");
                SocketChannel a = om.omOpenPort.accept();
                System.out.println("Got a client");
                om.clients.add(a);
            }catch(IOException e){
                logger.error("IOException with omOpenPort.accept()");
            }
        }
    }
}
