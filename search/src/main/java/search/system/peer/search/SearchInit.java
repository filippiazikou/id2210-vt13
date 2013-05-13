package search.system.peer.search;

import common.configuration.SearchConfiguration;
import common.peer.PeerAddress;
import se.sics.kompics.Init;

public final class SearchInit extends Init {

	private final PeerAddress peerSelf;
	private final int num;
	private final SearchConfiguration configuration;
    private final int partitionAmount;

//-------------------------------------------------------------------
	public SearchInit(PeerAddress peerSelf, int num, SearchConfiguration configuration, int partitionAmount) {
		super();
		this.peerSelf = peerSelf;
		this.num = num;
		this.configuration = configuration;
        this.partitionAmount = partitionAmount;
	}

    public int getPartitionAmount() {
        return partitionAmount;
    }

    //-------------------------------------------------------------------
	public PeerAddress getSelf() {
		return this.peerSelf;
	}

//-------------------------------------------------------------------
	public int getNum() {
		return this.num;
	}

//-------------------------------------------------------------------
	public SearchConfiguration getConfiguration() {
		return this.configuration;
	}
}