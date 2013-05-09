package tman.system.peer.tman;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/9/13
 * Time: 9:22 AM
 */
public class PingSchedule extends Timeout {
    public PingSchedule(ScheduleTimeout pingLeaderTimeout) {
        super(pingLeaderTimeout);
    }
}
