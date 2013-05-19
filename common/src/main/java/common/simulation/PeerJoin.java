package common.simulation;

import java.io.Serializable;

import se.sics.kompics.Event;

public final class PeerJoin extends Event implements Serializable{

	private final Long peerId;
	private final int num;

//-------------------------------------------------------------------	
	public PeerJoin(Long peerId, int num) {
		this.peerId = peerId;
		this.num = num;
	}

//-------------------------------------------------------------------	
	public Long getPeerId() {
		return this.peerId;
	}

//-------------------------------------------------------------------	
	public int getNum() {
		return this.num;
	}
}
