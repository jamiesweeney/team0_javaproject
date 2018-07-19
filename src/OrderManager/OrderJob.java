package OrderManager;

import java.util.ArrayList;
import java.util.List;


/** Contains the information for a new client order job. the method
 * that the job has to do and the arguments that it uses
 *
 */
public class OrderJob {

    String method;
    List<Object> args;

    OrderJob(String method, List<Object> args){
        this.method = method;
        this.args =  new ArrayList<>(args);
    }




}
