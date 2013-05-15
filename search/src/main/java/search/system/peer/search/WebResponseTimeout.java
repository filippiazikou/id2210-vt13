package search.system.peer.search;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.web.WebRequest;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/14/13
 * Time: 6:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebResponseTimeout extends Timeout {
    private WebRequest event;
    private String q;

    //-------------------------------------------------------------------
    public WebResponseTimeout(ScheduleTimeout request, WebRequest event , String q) {
        super(request);
        this.event = event;
        this.q = q;
    }

    public WebRequest getEvent() {
        return event;
    }

    public String getQ() {
        return q;
    }
}