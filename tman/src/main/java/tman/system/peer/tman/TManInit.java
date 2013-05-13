package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.PeerAddress;
import se.sics.kompics.Init;

public final class TManInit extends Init {

	private final PeerAddress peerSelf;
	private final TManConfiguration configuration;
    private final int excahngeSampleSize;
    private final int viewSize;
    private final int partitionAmount;

//-------------------------------------------------------------------
    public TManInit(PeerAddress peerSelf, TManConfiguration configuration, int excahngeSampleSize, int viewSize, int partitionAmount) {
        super();

        this.peerSelf = peerSelf;
        this.configuration = configuration;
        this.excahngeSampleSize = excahngeSampleSize;
        this.viewSize = viewSize;
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
	public TManConfiguration getConfiguration() {
		return this.configuration;
	}

    public int getExcahngeSampleSize() {
        return excahngeSampleSize;
    }

    public int getViewSize() {
        return viewSize;
    }
}