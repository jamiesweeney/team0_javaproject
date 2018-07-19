package Database;

import OrderManager.Order;

import java.sql.Timestamp;

public class ConvertDBOrder {

    // TODO Implement methods for conversion
    private Order convertDBOrderToOrder(DBOrderObject dbOrder) {
        return null;
    }
    private DBOrderObject convertOrderToDBOrder(Order order) {

        DBOrderObject dbOrderObject = new DBOrderObject();

        dbOrderObject.setId(null); // NULL so that it is uses auto-increment in database
        dbOrderObject.setDatetime(new Timestamp(System.currentTimeMillis()));
        dbOrderObject.setClientID(order.getClientID());
        //dbOrderObject.setRootOrderID();
        //dbOrderObject.setParentOrderID();
        //dbOrderObject.setMsgType();
        dbOrderObject.setOrderID(order.getClientOrderID());
        dbOrderObject.setOmID(order.getOmID());
        dbOrderObject.setInstrument(order.getInstrument());
        dbOrderObject.setOrdStatus(order.getOrdStatus());
        dbOrderObject.setSide(order.getSide());
        dbOrderObject.setOrderQty(order.getSize());
        //dbOrderObject.cumQty;
        //dbOrderObject.leavesQty;
        //dbOrderObject.price;

        return dbOrderObject;
    }
}
