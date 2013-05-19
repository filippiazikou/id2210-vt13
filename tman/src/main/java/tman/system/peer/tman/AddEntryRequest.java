package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.address.Address;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/10/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddEntryRequest extends PeerMessage {
    private String title;
    private String magnet;
    private UUID requestID;
    private PeerAddress initiator;


    public AddEntryRequest(PeerAddress source, PeerAddress destination, PeerAddress initiator, String title, String magnet, UUID requestID) {
        super(source, destination);
        this.title = title;
        this.magnet = magnet;
        this.requestID = requestID;
        this.initiator = initiator;
    }

    public String getMagnet() {
        return magnet;
    }

    public UUID getRequestID() {
        return requestID;
    }

    public PeerAddress getInitiator() {
        return initiator;
    }

    public String getTitle() {
        return title;
    }
}