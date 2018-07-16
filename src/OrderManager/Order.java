package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import Ref.Instrument;

public class Order implements Serializable {
    public long id;
    long orderRouter;
    public long clientOrderID;
    long size;
    double[] bestPrices;
    long bestPriceCount;

    public int sliceSizes() {
        int totalSizeOfSlices = 0;
        for (Order c : slices) totalSizeOfSlices += c.size;
        return totalSizeOfSlices;
    }

    public int newSlice(int sliceSize) {
        slices.add(new Order(id, clientOrderID, instrument, sliceSize, side));
        return slices.size() - 1;
    }

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

    public long sizeRemaining() {
        return size - sizeFilled();
    }

    long clientid;
    public Instrument instrument;
    int side;
    public double initialMarketPrice;
    ArrayList<Order> slices;
    ArrayList<Fill> fills;
    char OrdStatus = 'A'; //OrdStatus is Fix 39, 'A' is 'Pending New'

    //Status state;
    float price() {
        //TODO this is buggy as it doesn't take account of slices. Let them fix it
        float sum = 0;
        for (Fill fill : fills) {
            sum += fill.price;
        }
        return sum / fills.size();
    }

    void createFill(long size, double price) {
        fills.add(new Fill(size, price));
        if (sizeRemaining() == 0) {
            OrdStatus = '2'; // Fully Filled
        } else {
            OrdStatus = '1'; // Partially Filled
        }
    }

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

    void cancel() {
        //state=cancelled
    }

    public Order(long clientId, long ClientOrderID, Instrument instrument, int size, int side) {
        this.clientOrderID = ClientOrderID;
        this.size = size;
        this.clientid = clientId;
        this.instrument = instrument;
        this.side=side;
        fills = new ArrayList<Fill>();
        slices = new ArrayList<Order>();
    }
}

class Basket {
    Order[] orders;
}

class Fill implements Serializable {
    //long id;
    long size;
    double price;

    Fill(long size, double price) {
        this.size = size;
        this.price = price;
    }
}
