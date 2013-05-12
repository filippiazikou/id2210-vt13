package search.system.peer.search;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/12/13
 * Time: 9:27 AM
 */
public class AddEntryRequest extends PeerMessage {
    private String title;
    private String magnet;
    private int requestID;
    private PeerAddress initiator;

    public AddEntryRequest(PeerAddress source, PeerAddress destination, PeerAddress initiator, String title, String magnet, int requestID) {
        super(source, destination);
        this.title = title;
        this.magnet = magnet;
        this.requestID = requestID;
        this.initiator = initiator;
    }


    public String getMagnet() {
        return magnet;
    }

    public int getRequestID() {
        return requestID;
    }

    public PeerAddress getInitiator() {
        return initiator;
    }

    public String getTitle() {
        return title;
    }
}
