package common.simulation;

import se.sics.kompics.Event;

import java.io.Serializable;

public final class PeerFail extends Event implements Serializable {

	private final Long id;

//-------------------------------------------------------------------	
	public PeerFail(Long id) {
		this.id = id;
	}

//-------------------------------------------------------------------	
    public Long getId() {
        return id;
    }
}
