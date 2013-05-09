package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/9/13
 * Time: 9:41 AM
 */
public class IsLeaderSuspectedMessage extends PeerMessage {
    public IsLeaderSuspectedMessage(PeerAddress self, PeerAddress peerAddress) {
        super(self, peerAddress);
    }
}
