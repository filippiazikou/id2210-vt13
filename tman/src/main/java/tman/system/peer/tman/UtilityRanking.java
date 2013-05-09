package tman.system.peer.tman;

import common.peer.PeerAddress;
import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/8/13
 * Time: 9:34 AM
 */
public class UtilityRanking implements Comparator<PeerDescriptor> {

    @Override
    public int compare(PeerDescriptor peerDescriptor, PeerDescriptor peerDescriptor2) {
        return peerDescriptor.getPeerAddress().getPeerAddress().getId() - peerDescriptor2.getPeerAddress().getPeerAddress().getId();
    }
}
