package tman.system.peer.tman;

import se.sics.kompics.PortType;

public final class TManSamplePort extends PortType {{
	positive(TManSample.class);
    negative(AddEntryRequest.class);
    positive(AddEntryRequest.class);
    positive(AddEntryACK.class);
}}
