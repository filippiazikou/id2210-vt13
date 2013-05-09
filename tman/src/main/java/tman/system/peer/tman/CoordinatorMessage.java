package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/8/13
 * Time: 9:52 AM
 */
public class CoordinatorMessage extends PeerMessage {
    public CoordinatorMessage(PeerAddress source, PeerAddress destination) {
        super(source, destination);
    }
}
