package search.system.peer.search;

import common.peer.PeerAddress;
import common.peer.PeerMessage;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/14/13
 * Time: 6:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchResults extends PeerMessage {
    private Long eventId;
    private ArrayList<String> result;

    public SearchResults(PeerAddress source, PeerAddress destination, Long eventId, ArrayList<String> result) {
        super(source, destination);
        this.eventId = eventId;
        this.result = new ArrayList<String>();
        this.result = result;
    }

    public Long getEventId() {
        return eventId;
    }

    public ArrayList<String> getResult() {
        return result;
    }
}
