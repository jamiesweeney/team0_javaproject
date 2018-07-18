package OrderManager;

import java.io.Serializable;

public class Fill implements Serializable {
    //long id; //TODO Would this variable help with the issues Ryan+Jamie were discussing?
    long size;
    double price;

    Fill(long size, double price) {
        this.size = size;
        this.price = price;
    }
}
