package search.system.peer.search;

import common.peer.PeerAddress;
import common.peer.PeerMessage;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: kazarindn
 * Date: 4/22/13
 * Time: 7:50 PM
 */
public class GetUpdatesResponse extends PeerMessage {
    private ArrayList<BasicTorrentData> torrentData;

    public GetUpdatesResponse(PeerAddress source, PeerAddress destination, ArrayList<BasicTorrentData> torrentData) {
        super(source, destination);
        this.torrentData = torrentData;
    }

    public ArrayList<BasicTorrentData> getTorrentData() {
        return torrentData;
    }
}
