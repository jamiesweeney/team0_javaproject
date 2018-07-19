package Database;

import OrderManager.Order;

import java.sql.Date;

public class DBOrderObject {

    private int id;
    private Date datetime;
    private int clientID;
    private int rootOrderID;
    private int parentOrderID;
    private String msgType;
    private int orderID;
    // TODO Change this to be of type Instrument?
    // TODO Or at least ensure that DB entries match with numbering system used in Instrument class
    private int instrument;
    private char ordStatus;
    private String side;
    private int orderQty;
    private int cumQty;
    private int leavesQty;

    // TODO Implement methods for conversion
    private Order convertDBOrderToOrder(DBOrderObject dbOrder) {return null;};
    private DBOrderObject convertOrderToDBOrder(Order order) {return null;};
}
