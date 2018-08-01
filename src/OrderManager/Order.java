package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import Ref.Instrument;

public class Order implements Serializable {

    // Instance variables
    long clientid;
    public long clientOrderID;
    public Instrument instrument;

    public int routeCode;
    public int routerID;

    long size;
    int side;
    ArrayList<Fill> fills;
    ArrayList<Long> sliceIDs;
    ArrayList<Order> slices;
    long parentOID;

    public long uniqueOrderID;
    double[] bestPrices;
    long bestPriceCount;



    public double initialMarketPrice;
    char OrdStatus = 'A'; //OrdStatus is Fix 39, 'A' is 'Pending New'


    // Constructor
    public Order(long clientId, long ClientOrderID, Instrument instrument, int size, int side, Long pid) {
        this.clientOrderID = ClientOrderID;
        this.size = size;
        this.clientid = clientId;
        this.instrument = instrument;
        this.side=side;
        fills = new ArrayList<Fill>();
        sliceIDs = new ArrayList<>();
        if (pid != null) {
            parentOID = pid;
        }
    }


    // Returns the total of slice sizes i.e total size places
    public int sliceSizes() {
        int totalSizeOfSlices = 0;
        for (Order c : slices){
            totalSizeOfSlices += c.size;
        }
        return totalSizeOfSlices;
    }

    // Adds a new to the slice array, returns the slice index
    public int newSlice(int sliceSize) {
        slices.add(new Order(clientOrderID, clientOrderID, instrument, sliceSize, side, this.uniqueOrderID));
        return slices.size() - 1;
    }

    // Returns the total amount of the order filled
    public long sizeFilled() {
        int filledSoFar = 0;
        for (Fill f : fills) {
            filledSoFar += f.size;
        }
        for (Order c : slices) {
            filledSoFar += c.sizeFilled();
        }
        return filledSoFar;
    }

    // Returns the size left to fill
    public long sizeRemaining() {
        return size - sizeFilled();
    }


    /*currently returns average price of filled orders
    meant to return average price of all orders?
    now refactored to do so*/
    float price() {
        float sum = 0;
        for (Fill fill : fills) {
            sum += fill.price;
        }
        for (Order slice : slices) {
            sum += slice.price();
        }
        return sum / (fills.size() + slices.size()); //average price of all orders
    }


    // Makes a new fill
    void createFill(long size, double price) {
        fills.add(new Fill(size, price));
        if (sizeRemaining() == 0) {
            OrdStatus = '2'; // Fully Filled
        } else {
            OrdStatus = '1'; // Partially Filled
        }
    }

    //TODO: write a cancellation function in here? or just continue doing as is?

    void cross(Order matchingOrder) {
        //pair slices first and then parent
        for (Order slice : slices) {
            if (slice.sizeRemaining() == 0) continue;
            //TODO could optimise this to not start at the beginning every time <david note> look into class optimisation after refactoring
            for (Order matchingSlice : matchingOrder.slices) {
                long msze = matchingSlice.sizeRemaining();
                if (msze == 0) continue;
                long sze = slice.sizeRemaining();
                if (sze <= msze) {
                    slice.createFill(sze, initialMarketPrice);
                    matchingSlice.createFill(sze, initialMarketPrice);
                    break;
                }
                slice.createFill(msze, initialMarketPrice);
                matchingSlice.createFill(msze, initialMarketPrice);
            }
            long sze = slice.sizeRemaining();
            long mParent = matchingOrder.sizeRemaining() - matchingOrder.sliceSizes();
            if (sze > 0 && mParent > 0) {
                if (sze >= mParent) {
                    slice.createFill(sze, initialMarketPrice);
                    matchingOrder.createFill(sze, initialMarketPrice);
                } else {
                    slice.createFill(mParent, initialMarketPrice);
                    matchingOrder.createFill(mParent, initialMarketPrice);
                }
            }
            //no point continuing if we didn't fill this slice, as we must already have fully filled the matchingOrder
            if (slice.sizeRemaining() > 0) break;
        }
        if (sizeRemaining() > 0) {
            for (Order matchingSlice : matchingOrder.slices) {
                long msze = matchingSlice.sizeRemaining();
                if (msze == 0) continue;
                long sze = sizeRemaining();
                if (sze <= msze) {
                    createFill(sze, initialMarketPrice);
                    matchingSlice.createFill(sze, initialMarketPrice);
                    break;
                }
                createFill(msze, initialMarketPrice);
                matchingSlice.createFill(msze, initialMarketPrice);
            }
            long sze = sizeRemaining();
            long mParent = matchingOrder.sizeRemaining() - matchingOrder.sliceSizes();
            if (sze > 0 && mParent > 0) {
                if (sze >= mParent) {
                    createFill(sze, initialMarketPrice);
                    matchingOrder.createFill(sze, initialMarketPrice);
                } else {
                    createFill(mParent, initialMarketPrice);
                    matchingOrder.createFill(mParent, initialMarketPrice);
                }
            }
        }
    }
}

//class Basket
//{
//    Order[] orders;
//}

class Fill implements Serializable
{
    //long id;
    long size;
    double price;

    Fill(long size, double price)
    {
        this.size = size;
        this.price = price;
    }
}
