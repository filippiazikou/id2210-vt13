package tman.system.peer.tman;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/12/13
 * Time: 9:35 AM
 */

import common.peer.PeerAddress;
import common.peer.PeerMessage;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 10:57 PM
 * To change this template use File | Settings | File Templates.
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

