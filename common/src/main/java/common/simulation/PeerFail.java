package common.simulation;

import java.math.BigInteger;

import se.sics.kompics.Event;

public final class PeerFail extends Event {

	private final Long cyclonId;

//-------------------------------------------------------------------	
	public PeerFail(Long cyclonId) {
		this.cyclonId = cyclonId;
	}

//-------------------------------------------------------------------	
	public Long getCyclonId() {
		return cyclonId;
	}
}
