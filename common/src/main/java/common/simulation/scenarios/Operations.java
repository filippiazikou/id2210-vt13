package common.simulation.scenarios;

import common.simulation.*;

import java.math.BigInteger;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;

@SuppressWarnings("serial")
public class Operations {

  	public static Operation1<AddIndexEntry, Long> addIndexEntry() {
		return new Operation1<AddIndexEntry, Long>() {
                        @Override
			public AddIndexEntry generate(Long id) {
				return new AddIndexEntry(id);
			}
		};
	}
//-------------------------------------------------------------------
	public static Operation1<PeerJoin, Long> peerJoin(final int num) {
		return new Operation1<PeerJoin, Long>() {
			public PeerJoin generate(Long id) {
				return new PeerJoin(id, num);
			}
		};
	}


    public static Operation1<PeerFailLeader, Long> peerFailLeader = new Operation1<PeerFailLeader, Long>() {
        public PeerFailLeader generate(Long id) {
            return new PeerFailLeader(id);
        }
    };



    //-------------------------------------------------------------------
	public static Operation1<PeerFail, Long> peerFail = new Operation1<PeerFail, Long>() {
		public PeerFail generate(Long id) {
			return new PeerFail(id);
		}
	};

//-------------------------------------------------------------------
	public static Operation<Publish> publish = new Operation<Publish>() {
		public Publish generate() {
			return new Publish();
		}
	};
}
