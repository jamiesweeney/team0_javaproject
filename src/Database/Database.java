package Database;
/**
 * <h1>Database</h1>
 * Database is an interface, used as a point of access to the database written by Ryan Porteous.
 *
 * @author: Ryan Porteous
 * */
public interface Database
{
	/**
	 * The write() method is used to write data to the database.
	 * Current functionality is limited to displaying the data being written to the console.*/
	static void write(Object o)
	{
		System.out.println(o.toString());
	}
}