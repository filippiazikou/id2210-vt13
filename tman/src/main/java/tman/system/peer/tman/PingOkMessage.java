package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/9/13
 * Time: 9:29 AM
 */
public class PingOkMessage extends PeerMessage {
    public PingOkMessage(PeerAddress self, PeerAddress peerSource) {
        super(self, peerSource);
    }
}
