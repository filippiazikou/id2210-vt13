package tman.system.peer.tman;

import common.peer.PeerAddress;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 4/29/13
 * Time: 1:42 PM
 */
public class RankComparator implements Comparator<PeerDescriptor> {
    private PeerAddress self;

    public RankComparator(PeerAddress self) {
        this.self = self;
    }

    @Override
    public int compare(PeerDescriptor peerAddress, PeerDescriptor peerAddress2) {
        return rank(self, peerAddress.getPeerAddress(), peerAddress2.getPeerAddress());
    }

    //preference function
    private int rank(PeerAddress self, PeerAddress a, PeerAddress b) {
        int selfUtility = self.getPeerAddress().getId();
        int aUtility = a.getPeerAddress().getId();
        int bUtility = b.getPeerAddress().getId();

        int aDiffWithSelf = Math.abs(aUtility - selfUtility);
        int bDiffWithSelf = Math.abs(bUtility - selfUtility);

        return aDiffWithSelf - bDiffWithSelf;
    }
}
