package tman.system.peer.tman;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/19/13
 * Time: 9:32 AM
 */
public class DieEvent extends Timeout {
    public DieEvent(ScheduleTimeout dieTimeout) {
        super(dieTimeout);
    }
}
