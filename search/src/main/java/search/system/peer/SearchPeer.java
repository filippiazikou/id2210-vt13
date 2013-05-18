package search.system.peer;

import java.util.LinkedList;
import java.util.Set;

import common.configuration.TManConfiguration;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.P2pBootstrap;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClient;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClientInit;
import se.sics.kompics.timer.Timer;

import search.simulator.snapshot.Snapshot;
import common.peer.PeerAddress;
import search.system.peer.search.Search;
import search.system.peer.search.SearchInit;
import common.configuration.SearchConfiguration;
import common.configuration.CyclonConfiguration;
import cyclon.system.peer.cyclon.*;
import se.sics.kompics.web.Web;
import tman.system.peer.tman.TMan;
import tman.system.peer.tman.TManInit;
import tman.system.peer.tman.TManSamplePort;


public final class SearchPeer extends ComponentDefinition {

    	Positive<IndexPort> indexPort = positive(IndexPort.class);

        Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
        Negative<Web> webPort = negative(Web.class);
	
        private Component cyclon, tman, search, bootstrap;
	private int num;
	private Address self;
	private PeerAddress peerSelf;
	private int bootstrapRequestPeerCount;
	private boolean bootstrapped;
	private SearchConfiguration aggregationConfiguration;
    private int partitionAmount;

//-------------------------------------------------------------------	
	public SearchPeer() {
		cyclon = create(Cyclon.class);
		tman = create(TMan.class);
		search = create(Search.class);
		bootstrap = create(BootstrapClient.class);

		connect(network, search.getNegative(Network.class));
		connect(network, cyclon.getNegative(Network.class));
		connect(network, bootstrap.getNegative(Network.class));
		connect(network, tman.getNegative(Network.class));
		connect(timer, search.getNegative(Timer.class));
		connect(timer, cyclon.getNegative(Timer.class));
		connect(timer, bootstrap.getNegative(Timer.class));
		connect(timer, tman.getNegative(Timer.class));
		connect(webPort, search.getPositive(Web.class));
		connect(cyclon.getPositive(CyclonSamplePort.class), 
                        search.getNegative(CyclonSamplePort.class));
		connect(cyclon.getPositive(CyclonSamplePort.class), 
                        tman.getNegative(CyclonSamplePort.class));
		connect(tman.getPositive(TManSamplePort.class), 
                        search.getNegative(TManSamplePort.class));

                connect(indexPort, 
                        search.getNegative(IndexPort.class));
		
		subscribe(handleInit, control);
		subscribe(handleJoinCompleted, cyclon.getPositive(CyclonPort.class));
		subscribe(handleBootstrapResponse, bootstrap.getPositive(P2pBootstrap.class));
	}

//-------------------------------------------------------------------	
	Handler<SearchPeerInit> handleInit = new Handler<SearchPeerInit>() {
		public void handle(SearchPeerInit init) {
            partitionAmount = init.getPartitionAmount();
			num = init.getNum();
			peerSelf = init.getPeerSelf();
			self = peerSelf.getPeerAddress();
			CyclonConfiguration cyclonConfiguration = init.getCyclonConfiguration();
			aggregationConfiguration = init.getApplicationConfiguration();
			
			bootstrapRequestPeerCount = cyclonConfiguration.getBootstrapRequestPeerCount();

			trigger(new CyclonInit(cyclonConfiguration), cyclon.getControl());
			trigger(new BootstrapClientInit(self, init.getBootstrapConfiguration()), bootstrap.getControl());
			BootstrapRequest request = new BootstrapRequest("Cyclon", bootstrapRequestPeerCount);
			trigger(request, bootstrap.getPositive(P2pBootstrap.class));
			Snapshot.addPeer(peerSelf);
		}
	};


//-------------------------------------------------------------------	
	Handler<BootstrapResponse> handleBootstrapResponse = new Handler<BootstrapResponse>() {
		public void handle(BootstrapResponse event) {
			if (!bootstrapped) {
				Set<PeerEntry> somePeers = event.getPeers();
				LinkedList<PeerAddress> cyclonInsiders = new LinkedList<PeerAddress>();
				
				for (PeerEntry peerEntry : somePeers)
					cyclonInsiders.add((PeerAddress) peerEntry.getOverlayAddress());
				
				trigger(new CyclonJoin(peerSelf, cyclonInsiders), cyclon.getPositive(CyclonPort.class));
				bootstrapped = true;
			}
		}
	};

//-------------------------------------------------------------------	
	Handler<JoinCompleted> handleJoinCompleted = new Handler<JoinCompleted>() {
		public void handle(JoinCompleted event) {
			trigger(new BootstrapCompleted("Cyclon", peerSelf), bootstrap.getPositive(P2pBootstrap.class));
			trigger(new SearchInit(peerSelf, num, aggregationConfiguration, partitionAmount), search.getControl());
            trigger(new TManInit(peerSelf, new TManConfiguration(1000), 3, 5, partitionAmount), tman.getControl());
		}
	};
}
