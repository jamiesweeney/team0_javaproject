package OrderManager;

import java.util.ArrayList;
import java.util.List;

public class OrderJob {

    String method;
    List<Object> args;

    OrderJob(String method, List<Object> args){
        this.method = method;
        this.args =  new ArrayList<>(args);
    }




}
