import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;



/**
 * <h1>Mock</h1>
 * The Mock class is used solely to be inherited from and allow the user to call the show() method.
 * */


public class Mock
{
	private static Logger logger = Logger.getLogger(Mock.class);
	/**
	 *The show() method uses a Log4J logger to output a string to both the console and a log file.
	 * */
	public static void show(String out)
	{
		PropertyConfigurator.configure("resources/log4j.properties");
		logger.info(Thread.currentThread().getName()+":"+out);
	}
}