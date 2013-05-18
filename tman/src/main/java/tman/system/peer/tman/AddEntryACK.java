package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.web.WebRequest;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 10:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddEntryACK extends PeerMessage {
    private UUID requestID;
    PeerAddress initiator;
    private Integer entryId;

    public AddEntryACK(PeerAddress source, PeerAddress destination, PeerAddress initiator, UUID requestID, Integer entryId) {
        super(source, destination);
        this.requestID = requestID;
        this.initiator = initiator;
        this.entryId = entryId;
    }

    public UUID getRequestID() {
        return requestID;
    }

    public PeerAddress getInitiator() {
        return initiator;
    }

    public Integer getEntryId() {
        return entryId;
    }
}
