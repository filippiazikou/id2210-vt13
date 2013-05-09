package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/9/13
 * Time: 9:27 AM
 */
public class PingMessage extends PeerMessage {
    public PingMessage(PeerAddress self, PeerAddress leader) {
        super(self, leader);
    }
}
