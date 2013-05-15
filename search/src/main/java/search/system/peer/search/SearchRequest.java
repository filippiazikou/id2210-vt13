package search.system.peer.search;

import common.peer.PeerAddress;
import common.peer.PeerMessage;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/14/13
 * Time: 6:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchRequest extends PeerMessage {
    private Long eventId;
    private String q;

    public SearchRequest(PeerAddress source, PeerAddress destination, Long eventId, String q) {
        super(source, destination);
        this.eventId = eventId;
        this.q = q;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getQ() {
        return q;
    }
}
