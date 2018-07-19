package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import Ref.Instrument;


/**
 *<h1>Order</h1>
 * The order class implements Serializable and is used as the standard format for orders.
 * */
public class Order implements Serializable {

    // Instance variables
    private long clientID; // Unique ID of a client
    //
    //
    //
    private long clientOrderID; // Unique orderID for a client
    private long omID; // Unique orderID within entire Order Manager DB
    private Instrument instrument;
    private char ordStatus = 'A'; // ordStatus is Fix 39, 'A' is 'Pending New'
    private int side; // Buy Side or Sell Side / 1 or 2
    private long size; // Size of order

    // TODO Refactor these out? Worth the effort?
    private ArrayList<Fill> fills;
    private ArrayList<Order> slices;


    private int routeCode;
    private int routerID;
    //long orderRouter; TODO - Do we need this?
    private double[] bestPrices;
    private long bestPriceCount;
    //    Status state;
    private double initialMarketPrice;


    /** The constructer for the order object
     *
     * @param clientId the id of the client that the order belongs to
     * @param ClientOrderID the unique id for the client order
     * @param instrument the instrument that the order was for
     * @param size the size of the order
     * @param side 1 for buy, 2 for sell
     */
    public Order(long clientId, long ClientOrderID, Instrument instrument, int size, int side) {
        this.clientOrderID = ClientOrderID;
        this.size = size;
        this.clientID = clientID;
        this.instrument = instrument;
        this.side=side;
        fills = new ArrayList<Fill>();
        slices = new ArrayList<Order>();
    }



    /** Returns the total of slice sizes i.e total size places
     *
     * @return int the total size of all slices
     */
    public int sliceSizes() {
        int totalSizeOfSlices = 0;
        for (Order c : slices){
            totalSizeOfSlices += c.size;
        }
        return totalSizeOfSlices;
    }

    /** Creates a new slice object
     *
     * @param sliceSize the amount that we wish to slice off
     * @return
     */
    // Adds a new to the slice array, returns the slice index
    public int newSlice(int sliceSize) {
        slices.add(new Order(omID, clientOrderID, instrument, sliceSize, side));
        return slices.size() - 1;
    }

    /** Returns the amount of the order that has already been filled
     *
     * @return int the amount filled already
     */
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


    /** Returns the amount of the order that is yet to fill
     *
     * @return int the amount of order still to be filled
     */
    public long sizeRemaining() {
        return size - sizeFilled();
    }


    /**
     *
      * @return
     */
    float price() {
        //TODO this is buggy as it doesn't take account of slices. Let them fix it
        float sum = 0;
        for (Fill fill : fills) {
            sum += fill.price;
        }
        return sum / fills.size();
    }


    /** Partially fills the order
     *
     * @param size size of the order to be filled
     * @param price price of the order to be filled
     */
    // Makes a new fill
    void createFill(long size, double price) {
        fills.add(new Fill(size, price));
        if (sizeRemaining() == 0) {
            ordStatus = '2'; // Fully Filled
        } else {
            ordStatus = '1'; // Partially Filled
        }
    }

    /** Crosses the orded with a matching order
     *
     * @param matchingOrder the matching order that we wish to cross with
     */
    void cross(Order matchingOrder) {
        //pair slices first and then parent
        for (Order slice : slices) {
            if (slice.sizeRemaining() == 0) continue;
            //TODO could optimise this to not start at the beginning every time
            for (Order matchingSlice : matchingOrder.slices) {
                long msze = matchingSlice.sizeRemaining();
                if (msze == 0) continue;
                long sze = slice.sizeRemaining();
                if (sze <= msze) {
                    slice.createFill(sze, initialMarketPrice);
                    matchingSlice.createFill(sze, initialMarketPrice);
                    break;
                }
                //sze>msze
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
                //sze>msze
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

    // TODO - add functionality
    void cancel() {
        //state=cancelled
    }

    public long getClientID() {
        return this.clientID;
    }

    public long getClientOrderID() {
        return this.clientOrderID;
    }

    public Instrument getInstrument() {
        return this.instrument;
    }

    public int getRouteCode() {
        return this.routeCode;
    }

    public void setRouteCode(int routeCode) {
        this.routeCode = routeCode;
    }

    public int getRouterID() {
        return this.routerID;
    }

    public void setRouterID(int routerID) {
        this.routerID = routerID;
    }

    public long getSize() {
        return this.size;
    }

    public int getSide() {
        return this.side;
    }

    public ArrayList<Fill> getFills() {
        return this.fills;
    }

    public ArrayList<Order> getSlices() {
        return this.slices;
    }

    public long getOmID() {
        return this.omID;
    }

    public double[] getBestPrices() {
        return this.bestPrices;
    }

    public void setBestPrices(double[] bestPrices) {
        this.bestPrices = bestPrices;
    }

    public long getBestPriceCount() {
        return this.bestPriceCount;
    }

    public void setBestPriceCount(long bestPriceCount) {
        this.bestPriceCount = bestPriceCount;
    }
    public double getInitialMarketPrice() {
        return this.initialMarketPrice;
    }

    public char getOrdStatus() {
        return this.ordStatus;
    }

    public void setOrdStatus(char ordStatus) {
        this.ordStatus = ordStatus;
    }
}