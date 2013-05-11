package search.system.peer.search;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class GarbageRequestIdTimeout  extends Timeout  {
     int requestId;
    //-------------------------------------------------------------------
    public GarbageRequestIdTimeout(ScheduleTimeout request, int requestId) {
        super(request);
        this.requestId = requestId;
    }

    public int getRequestId() {
        return requestId;
    }
}
