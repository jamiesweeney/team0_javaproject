import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Random;

import OrderClient.Client;
import OrderClient.NewOrderSingle;
import OrderManager.Order;
import Ref.Instrument;
import Ref.Ric;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class SampleClient extends Mock implements Client
{
	private static final Random RANDOM_NUM_GENERATOR = new Random();

	private static final Instrument[] INSTRUMENTS = {new Instrument(new Ric("VOD.L")),
			                                         new Instrument(new Ric("BP.L")),
			                                         new Instrument(new Ric("BT.L"))};


	private static final HashMap OUT_QUEUE = new HashMap(); //queue for outgoing orders

	private int id = 0; //message id number

	private SocketChannel omConn; //connection to order manager


	private Logger logger = Logger.getLogger(SampleClient.class);



	//Constructor used to push connection to OrderManager, and not receive it.
	public SampleClient()
	{
		PropertyConfigurator.configure("resources/log4j.properties");

		System.out.println("conntected ");

		omConn = connect(new InetSocketAddress("localhost", 2025));
		System.out.println("conntected ");

		if(omConn == null)
		{
			logger.fatal("Client didn't connect after 200 attempts.");
		}
	}

//	public SampleClient(int port) throws IOException
//	{
//		PropertyConfigurator.configure("resources/log4j.properties");
//
//		//OM will connect to us
//		omConn=new ServerSocket(port).accept();
//		logger.info("OM connected to client port "+port);
//	}


	public SocketChannel connect(InetSocketAddress serverSocket)
	{
		//Replication of OM connector code.
		//Attempt to connect 200 times before returning an error.
		int tryCounter = 0;
		SocketChannel s = null;
		while(tryCounter < 200)
		{
			try
			{
				s = SocketChannel.open();
				s.connect(new InetSocketAddress("localhost", 2025));
				break;
			}
			catch(IOException e)
			{
				logger.error("Client not connected to server!");
				tryCounter++;
			}
		}
		return s;
	}




	public int sendRandomOrder() throws IOException
	{
		// Generate some data
		int size=100;
		float price = (float)RANDOM_NUM_GENERATOR.nextInt(100);
		int instid = RANDOM_NUM_GENERATOR.nextInt(3);
		Instrument instrument=INSTRUMENTS[instid];
		int side = RANDOM_NUM_GENERATOR.nextInt(2) + 1;

		// Make a new order single
		NewOrderSingle nos = new NewOrderSingle(size,price,instrument,side);

		// Adding order to queue
		show("sendOrder: id="+id+" size="+size+" price="+price+" instrument="+INSTRUMENTS[instid].toString()+" side="+side);
		OUT_QUEUE.put(id,nos);

		System.out.println("jsajsajsaj");

		// Write the order
		// newOrderSingle; 35=D; id; nos;
		if(omConn.isConnected())
		{

			System.out.println("SENDING");
			ObjectOutputStream os=new ObjectOutputStream(omConn.socket().getOutputStream());
			os.writeObject("newOrderSingle");
			//os.writeObject("35=D;"); TODO - Work out why this crashes
			os.writeInt(id);
			os.writeObject(nos);
			os.flush();
		}
		return id++;
	}




	@Override
	public int sendOrder(int id, int size, char msgType, float price, Instrument ins, int side)throws IOException
	{
        // Make a new order single
		NewOrderSingle nos = new NewOrderSingle(size,price,ins,side);

		// Adding order to queue
		show("sendOrder: id="+id+" size="+size+" msgType="+ins.toString()+" price="+price+" instrument="+ins+" side="+side);
		OUT_QUEUE.put(id,nos);


		// Write the order
        // newOrderSingle; 35=D; id; nos;
		if(omConn.isConnected())
		{
			System.out.println("SENDINGSENDINGSENDINGSENDINGSENDING");
			ObjectOutputStream os=new ObjectOutputStream(omConn.socket().getOutputStream());
			os.writeObject("newOrderSingle");
			//os.writeObject("35=D;"); TODO - Work out why this crashes
			os.writeInt(id);
			os.writeObject(nos);
			os.flush();
		}
		return id++;
	}

	@Override
	public void sendCancel(int idToCancel) {
		show("sendCancel: id=" + idToCancel);
		if (omConn.isConnected()) {
			// OMconnection.sendMessage("cancel",idToCancel);
			try {
				ObjectOutputStream os = new ObjectOutputStream(omConn.socket().getOutputStream());
				os.writeObject("sendCancel");
				os.writeInt(idToCancel);
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void partialFill(Order order)
	{
		show(""+order);
	}

	@Override
	public void fullyFilled(Order order)
	{
		show(""+order);
		OUT_QUEUE.remove(order.clientOrderID);
	}

	@Override
	public void cancelled(Order order)
	{
		show(""+order);
		OUT_QUEUE.remove(order.clientOrderID);
	}

	enum methods{newOrderSingleAcknowledgement,dontKnow, orderCancellationSuccessful, orderCancellationRejected};

	@Override
	public void messageHandler(){
		
		ObjectInputStream is;
		try {
			while(true)
			{
				//is.wait(); //this throws an exception!!
				while(0<omConn.socket().getInputStream().available())
				{
					is = new ObjectInputStream(omConn.socket().getInputStream());

					String fix=(String)is.readObject();

					logger.info(Thread.currentThread().getName()+" received fix message: "+fix);

					String[] fixTags=fix.split(";");
					int OrderId=-1;

					char MsgType;

					int OrdStatus;

					methods whatToDo = methods.dontKnow;

					//String[][] fixTagsValues=new String[fixTags.length][2];

					for(int i=0;i<fixTags.length;i++)
					{
						String[] tag_value=fixTags[i].split("=");
						switch(tag_value[0])
						{
							case"11":OrderId=Integer.parseInt(tag_value[1]);break;
							case"35":MsgType=tag_value[1].charAt(0);
								if(MsgType=='A')whatToDo=methods.newOrderSingleAcknowledgement;
								if(MsgType=='9')whatToDo= methods.orderCancellationRejected;
								break;
							case"39":OrdStatus=tag_value[1].charAt(0);
								if(OrdStatus=='4')whatToDo=methods.orderCancellationSuccessful;
							break;

						}
					}
					switch(whatToDo)
					{
						case newOrderSingleAcknowledgement:newOrderSingleAcknowledgement(OrderId);
						case orderCancellationSuccessful:orderCancelSuccessful();
					}
					
					/*message=connection.getMessage();
					char type;
					switch(type){
						case 'C':cancelled(message);break;
						case 'P':partialFill(message);break;
						case 'F':fullyFilled(message);
					}*/
//					show("");
				}
			}
		}
		catch (IOException|ClassNotFoundException e)
		{
			logger.error("Exception caught: " + e);
			e.printStackTrace();
		}
	}

	void newOrderSingleAcknowledgement(int OrderId){
		logger.info(Thread.currentThread().getName()+" called newOrderSingleAcknowledgement");
		//do nothing, as not recording so much state in the NOS class at present
	}
	void orderCancelSuccessful() {
		logger.info(Thread.currentThread().getName()+" called order Cancel Successful");
	}
	void orderCancellationRejected(){
		logger.info(Thread.currentThread().getName()+" called order cancellation rejected");
	}
/*listen for connections
once order manager has connected, then send and cancel orders randomly
listen for messages from order manager and print them to stdout.*/
}