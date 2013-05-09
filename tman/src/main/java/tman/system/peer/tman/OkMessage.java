package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/8/13
 * Time: 10:09 AM
 */
public class OkMessage extends PeerMessage {
    public OkMessage(PeerAddress source, PeerAddress destination) {
        super(source, destination);
    }
}
