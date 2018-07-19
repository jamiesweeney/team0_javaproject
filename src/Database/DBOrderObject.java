package Database;

import Ref.Instrument;
import java.sql.Timestamp;

public class DBOrderObject {

    private Integer id;
    private Timestamp datetime;
    private long clientID;
    private long rootOrderID;
    private long parentOrderID;
    private String msgType;
    private long orderID;
    private long omID;
    // TODO Change this to be of type Instrument?
    // TODO Or at least ensure that DB entries match with numbering system used in Instrument class
    private Instrument instrument;
    private char ordStatus;
    private int side;
    private long orderQty;
    private long cumQty;
    private long leavesQty;
    private float price;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getDatetime() {
        return datetime;
    }

    public void setDatetime(Timestamp datetime) {
        this.datetime = datetime;
    }

    public long getClientID() {
        return clientID;
    }

    public void setClientID(long clientID) {
        this.clientID = clientID;
    }

    public long getRootOrderID() {
        return rootOrderID;
    }

    public void setRootOrderID(long rootOrderID) {
        this.rootOrderID = rootOrderID;
    }

    public long getParentOrderID() {
        return parentOrderID;
    }

    public void setParentOrderID(long parentOrderID) {
        this.parentOrderID = parentOrderID;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public long getOrderID() {
        return orderID;
    }

    public void setOrderID(long orderID) {
        this.orderID = orderID;
    }

    public long getOmID() {
        return omID;
    }

    public void setOmID(long omID) {
        this.omID = omID;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public char getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(char ordStatus) {
        this.ordStatus = ordStatus;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    public long getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(long orderQty) {
        this.orderQty = orderQty;
    }

    public long getCumQty() {
        return cumQty;
    }

    public void setCumQty(long cumQty) {
        this.cumQty = cumQty;
    }

    public long getLeavesQty() {
        return leavesQty;
    }

    public void setLeavesQty(long leavesQty) {
        this.leavesQty = leavesQty;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}