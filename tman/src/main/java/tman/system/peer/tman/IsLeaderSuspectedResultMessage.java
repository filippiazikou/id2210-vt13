package tman.system.peer.tman;

import common.peer.PeerAddress;
import common.peer.PeerMessage;
import se.sics.kompics.Event;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/9/13
 * Time: 9:44 AM
 */
public class IsLeaderSuspectedResultMessage extends PeerMessage {
    private final boolean isLeaderSuspected;

    public IsLeaderSuspectedResultMessage(PeerAddress self, PeerAddress peerSource, boolean leaderSuspected) {
        super(self, peerSource);

        this.isLeaderSuspected = leaderSuspected;
    }

    public boolean isLeaderSuspected() {
        return isLeaderSuspected;
    }
}
