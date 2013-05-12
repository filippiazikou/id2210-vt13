package search.system.peer.search;

import common.peer.PeerAddress;
import common.peer.PeerMessage;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/12/13
 * Time: 9:28 AM
 */
public class AddEntryACK extends PeerMessage {
    private int requestID;
    PeerAddress initiator;
    public AddEntryACK(PeerAddress source, PeerAddress destination, PeerAddress initiator, int requestID) {
        super(source, destination);
        this.requestID = requestID;
        this.initiator = initiator;
    }

    public int getRequestID() {
        return requestID;
    }

    public PeerAddress getInitiator() {
        return initiator;
    }
}
