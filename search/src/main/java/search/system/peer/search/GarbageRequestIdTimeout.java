package search.system.peer.search;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 5:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class GarbageRequestIdTimeout  extends Timeout  {
     UUID requestId;
    //-------------------------------------------------------------------
    public GarbageRequestIdTimeout(ScheduleTimeout request, UUID requestId) {
        super(request);
        this.requestId = requestId;
    }

    public UUID getRequestId() {
        return requestId;
    }
}
