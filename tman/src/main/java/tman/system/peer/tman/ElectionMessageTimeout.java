package tman.system.peer.tman;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/8/13
 * Time: 10:35 AM
 */
public class ElectionMessageTimeout extends Timeout {
    protected ElectionMessageTimeout(ScheduleTimeout request) {
        super(request);
    }
}
