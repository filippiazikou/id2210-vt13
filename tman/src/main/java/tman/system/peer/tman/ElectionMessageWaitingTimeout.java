package tman.system.peer.tman;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/8/13
 * Time: 10:43 AM
 */
public class ElectionMessageWaitingTimeout extends Timeout {
    public ElectionMessageWaitingTimeout(ScheduleTimeout rst) {
        super(rst);
    }
}
