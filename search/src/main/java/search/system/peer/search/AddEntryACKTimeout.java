package search.system.peer.search;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 11:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddEntryACKTimeout extends Timeout {
    private UUID requestId;
    private String title;
    private String magnet;
    //-------------------------------------------------------------------
    public AddEntryACKTimeout(ScheduleTimeout request, UUID requestId, String title, String magnet) {
        super(request);
        this.requestId = requestId;
        this.title = title;
        this.magnet = magnet;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public String getTitle() {
        return title;
    }

    public String getMagnet() {
        return magnet;
    }
}