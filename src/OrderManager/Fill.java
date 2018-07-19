package OrderManager;

import java.io.Serializable;

/**
 * <h1>Fill</h1>
 *
 * The Fill class just holds size and price data.
 * */
public class Fill implements Serializable {

    //long id;
    long size;
    double price;

    /**
     * The Fill constructor takes in 2 arguments
     * */
    Fill(long size, double price)
    {
        this.size = size;
        this.price = price;
    }
}
