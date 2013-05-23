package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 5/23/13
 * Time: 10:38 AM
 */
public class LinearComparator implements Comparator<PeerDescriptor> {
    @Override
    public int compare(PeerDescriptor peerDescriptor, PeerDescriptor peerDescriptor2) {
        if(peerDescriptor.getPeerAddress().getPeerAddress().getId() < peerDescriptor2.getPeerAddress().getPeerAddress().getId())
            return -1;
        else
            return 1;
    }
}
