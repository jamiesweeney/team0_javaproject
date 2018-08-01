import OrderManager.OrderManager;

import org.apache.log4j.Logger;

public class ExceptionCatcher {


    Logger logger = Logger.getLogger(OrderManager.class);

    public void displayException(Exception e){
        logger.error(e.getMessage() + e);
    }
}
