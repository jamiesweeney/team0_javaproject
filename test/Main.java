import java.io.IOException;
import java.net.InetSocketAddress;


import Ref.Instrument;
import Ref.Ric;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator ;

import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;


/**
* <h1>Main</h1>
 * The Main class implements the main() method, which is that starting point of the application.
 * <p>
 *  In the main() method, several Client objects are created, as well as Router objects and a Trader.
 *  These objects are linked to a unique InetSocketAddress, which is used by the objects to communicate
 *  and transfer data.
 *  <p>
 *  Lastly, sample market data is created, and an OrderManager is created.
*/
public class Main
{
	public static void main(String[] args) throws IOException
	{
		//Create main logging object.
		Logger logger = Logger.getLogger(Main.class);
		//Configure Log4J using the xml file in /resources.
		PropertyConfigurator.configure("resources/log4j.properties");

		logger.info("TEST: this program tests OrderManager");

		//start sample clients
		//new MockClient("Client 1",2000).start();
		//new MockClient("Client 2",2001).start();

		new MockClient("Client 3").start();

		//start sample routers
		new SampleRouter("Router LSE",2010).start();
		new SampleRouter("Router BATE",2011).start();

		new Trader("Trader James",2020).start();

		//start order manager
		InetSocketAddress[] clients = {new InetSocketAddress("localhost",2000),
				                       new InetSocketAddress("localhost",2001)};

		InetSocketAddress[] routers = {new InetSocketAddress("localhost",2010),
				                       new InetSocketAddress("localhost",2011)};

		InetSocketAddress trader = new InetSocketAddress("localhost",2020);

		LiveMarketData liveMarketData = new SampleLiveMarketData();

		//new MockOM("Order Manager",routers,clients,trader,liveMarketData).start();
		new MockOM("MockOM").start();
	}
}


/**
 * The MockClient class is used to simulate an actual client and is used for program testing.
 * */

class MockClient extends Thread
{
	private Logger logger = Logger.getLogger(MockClient.class);

	int port;

	MockClient(String name)
	{
		this.setName(name);
	}

	MockClient(String name,int port)
	{
		this.port=port;
		this.setName(name);
	}


	/**
	 *The run() method is implemented as teh class extends Thread. This method creates a new Client object, which sends orders to the
	 * OrderManager. The code has been modified to allow the client to send either a random order or a specific order by inputting manual values.
	 * */
	public void run()
	{
		try
		{
			PropertyConfigurator.configure("resources/log4j.properties");
			SampleClient client = new SampleClient();
			//SampleClient client=new SampleClient(port);

			if(port==2000)
			{
				//client.sendOrder(int id, int size, char msgType, float price, Instrument ins, int side);
				//client.sendOrder(0, 100, 'D', 8.0f, new Instrument(new Ric("VOD.L")), 1);
				client.sendRandomOrder();

				int id=client.sendRandomOrder();

				client.sendCancel(id);
				client.messageHandler();
			}
			else
			{
				client.sendRandomOrder();
				client.messageHandler();
			}

		}
		catch (IOException e)
		{
			logger.error("IOException caught: look into run method of MockClient: " + e);
		}
	}
}





/**
 * The MockOM class extends Thread anf is used to initialise an OrderManager object for testing.
 * */
class MockOM extends Thread
{
	private Logger logger = Logger.getLogger(MockOM.class);

	InetSocketAddress[] clients;
	InetSocketAddress[] routers;

	InetSocketAddress trader;

	LiveMarketData liveMarketData;

	MockOM(String name,
		   InetSocketAddress[] routers,
		   InetSocketAddress[] clients,
		   InetSocketAddress trader,
		   LiveMarketData liveMarketData)
	{
		this.clients=clients;
		this.routers=routers;
		this.trader=trader;
		this.liveMarketData=liveMarketData;
		this.setName(name);
	}
	MockOM(String name)
	{
		this.setName(name);
	}

	@Override
	public void run()
	{
		PropertyConfigurator.configure("resources/log4j.properties");

		//In order to debug constructors you can do F5 F7 F5
		new OrderManager(routers,clients,trader,liveMarketData);
	}
}