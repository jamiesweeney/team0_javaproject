import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Mock
{
	private static Logger logger = Logger.getLogger(Mock.class);

	public static void show(String out)
	{
		PropertyConfigurator.configure("resources/log4j.properties");
		logger.info(Thread.currentThread().getName()+":"+out);
		//System.err.println(Thread.currentThread().getName()+":"+out);
	}
}