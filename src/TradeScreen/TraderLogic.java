package TradeScreen;

import OrderManager.Order;
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class TraderLogic extends Thread implements TradeScreen, Runnable{
    static String name;
    static int port;
    private HashMap<Integer,Order> orders=new HashMap<Integer,Order>();
    private static Socket omConn;
    ObjectInputStream is;
    ObjectOutputStream os;

    private Screen screen;

    public TraderLogic(String name, int port) {
        this.name = name;
        this.port = port;
        screen = new Screen();
    }

    @Override
    public void run() {
        try {
            traderLogic();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void traderLogic()  throws IOException, ClassNotFoundException, InterruptedException  {

        omConn= ServerSocketFactory.getDefault().createServerSocket(port).accept();

        InputStream s=omConn.getInputStream(); //if i try to create an objectinputstream before we have data it will block
        while(true){
            //System.out.println("order");
            if(0<s.available()){
                is=new ObjectInputStream(s);  //TODO check if we need to create each time. this will block if no data, but maybe we can still try to create it once instead of repeatedly
                TradeScreen.api method=(TradeScreen.api)is.readObject();
                System.out.println(Thread.currentThread().getName()+" calling: "+method);
                switch(method){
                    case newOrder:newOrder(is.readInt(),(Order)is.readObject());break;
                    case price:price(is.readInt(),(Order)is.readObject());break;
                    case cross:is.readInt();is.readObject();break; //TODO
                    case fill:is.readInt();is.readObject();break; //TODO
                }
            }
        }
    }

    @Override
    public void newOrder(int id, Order order) throws IOException, InterruptedException {
        Thread.sleep(2134);
        orders.put(id, order);
        screen.addOrder(order);
        //acceptOrder(id);
    }

    @Override
    public void acceptOrder(int id) throws IOException {
        os=new ObjectOutputStream(omConn.getOutputStream());
        os.writeObject("acceptOrder");
        os.writeInt(id);
        os.flush();
    }

    @Override
    public void sliceOrder(int id, int sliceSize) throws IOException {
        os=new ObjectOutputStream(omConn.getOutputStream());
        os.writeObject("sliceOrder");
        os.writeInt(id);
        os.writeInt(sliceSize);
        os.flush();
    }

    @Override
    public void price(int id, Order o) throws InterruptedException, IOException {
        Thread.sleep(2134);
        sliceOrder(id,orders.get(id).sizeRemaining()/2);
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }
}
