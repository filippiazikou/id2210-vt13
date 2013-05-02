package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import common.peer.PeerAddress;

import java.util.*;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;

import cyclon.system.peer.cyclon.DescriptorBuffer;
import cyclon.system.peer.cyclon.PeerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);
    Negative<TManSamplePort> tmanPartnersPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private long period;
    private PeerAddress self;
    private ArrayList<PeerAddress> tmanPartners;
    private TManConfiguration tmanConfiguration;
    private int c = 5;
    private int m = 3;

    private ArrayList<PeerDescriptor> view = new ArrayList<PeerDescriptor>();
    private ArrayList<PeerDescriptor> buffer = new ArrayList<PeerDescriptor>();
    Random rnd = new Random();

    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

//-------------------------------------------------------------------
        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

//-------------------------------------------------------------------	
    public TMan() {
        tmanPartners = new ArrayList<PeerAddress>();

        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManPartnersResponse, networkPort);
        subscribe(handleTManPartnersRequest, networkPort);
    }
//-------------------------------------------------------------------	
    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);

        }
    };
//-------------------------------------------------------------------	
    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            tmanPartners = getPartners();
            Snapshot.updateTManPartners(self, tmanPartners);

            // Publish sample to connected components
            trigger(new TManSample(tmanPartners), tmanPartnersPort);
        }
    };

    private ArrayList<PeerAddress> getPartners() {
        ArrayList<PeerAddress> result = new ArrayList<PeerAddress>();

        for(int i=0; i<view.size(); i++)
            result.add(view.get(i).getPeerAddress());

        return result;
    }

    //-------------------------------------------------------------------
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            ArrayList<PeerAddress> cyclonPartners = event.getSample();
            if(cyclonPartners.size() == 0) return;

            PeerDescriptor q = selectPeerFromView();
            buffer = merge(view, new ArrayList<PeerDescriptor>(){{new PeerDescriptor(self);}});

            PeerAddress rndPeer = cyclonPartners.get(rnd.nextInt(cyclonPartners.size()));
            ArrayList<PeerDescriptor> myDescriptor = new ArrayList<PeerDescriptor>();
            myDescriptor.add(new PeerDescriptor(rndPeer));
            buffer = merge(buffer, myDescriptor);

            if(q == null) {
                Collections.sort(buffer, new RankComparator(rndPeer));
                trigger(new ExchangeMsg.Request(UUID.randomUUID(), new DescriptorBuffer(self, takeM(buffer)), self, rndPeer), networkPort);
            }
            else {
                Collections.sort(buffer, new RankComparator(q.getPeerAddress()));
                trigger(new ExchangeMsg.Request(UUID.randomUUID(), new DescriptorBuffer(self, takeM(buffer)), self, q.getPeerAddress()), networkPort);
            }
        }
    };
//-------------------------------------------------------------------
    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            buffer = merge(view, new ArrayList<PeerDescriptor>(){{new PeerDescriptor(self);}});
            Collections.sort(buffer, new RankComparator(event.getPeerSource()));

            trigger(new ExchangeMsg.Response(UUID.randomUUID(), new DescriptorBuffer(self, takeM(buffer)), self, event.getPeerSource()), networkPort);
            buffer = merge(event.getRandomBuffer().getDescriptors(), view);

            Collections.sort(buffer, new RankComparator(self));
            view = selectView();
        }
    };
    
    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {

            buffer = merge(event.getSelectedBuffer().getDescriptors(), view);


            Collections.sort(buffer, new RankComparator(self));
            view = selectView();

            logger.info("============= View =============");
            for(int i=0; i<view.size(); i++) {
                logger.info(String.format("%s partner - %s", self.getPeerAddress().getId(), view.get(i).getPeerAddress().getPeerAddress().getId()));
            }
            logger.info("++++++++++++++++++++++++++++++++++++");
        }
    };

    private ArrayList<PeerDescriptor> takeM(ArrayList<PeerDescriptor> buffer) {
        if(buffer.size() <= m)
            return buffer;

        ArrayList<PeerDescriptor> result = new ArrayList<PeerDescriptor>();
        for(int i=0; i<m; i++)
            result.add(buffer.get(i));
        return result;
    }

    private ArrayList<PeerDescriptor> selectView() {
        PeerDescriptor own = null;
        for(int i = 0; i < buffer.size(); i++) {
            if(buffer.size() < 3) break;
            if(buffer.get(i).getPeerAddress().getPeerAddress().equals(self.getPeerAddress())) {
                own = buffer.get(i);
                break;
            }
        }

        if(own != null)
            buffer.remove(own);

        if(buffer.size() <= c)
            return buffer;

        ArrayList<PeerDescriptor> view = new ArrayList<PeerDescriptor>();
        for(int i=0; i<c; i++)
            view.add(buffer.get(i));

        return view;
    }

    //select peer from view
    private PeerDescriptor selectPeerFromView() {
        if(view.size() == 0)
            return null;

        if(view.size() == 1)
            return view.get(0);

        Collections.sort(view, new RankComparator(self));
        int halfViewSize = view.size() / 2;
        int selectedPeer = rnd.nextInt(halfViewSize);

        return view.get(selectedPeer);
    }

    //merge function
    private ArrayList<PeerDescriptor> merge(ArrayList<PeerDescriptor> first, ArrayList<PeerDescriptor> second) {
        if(first.size() == 0)
            return second;

        ArrayList<PeerDescriptor> result = new ArrayList<PeerDescriptor>();

        for(int i=0; i<first.size(); i++) {
            boolean found = false;
            for (int j=0; j<second.size(); j++) {
                if(first.get(i).getPeerAddress().equals(second.get(j).getPeerAddress())) {
                    if(first.get(i).getAge() >= second.get(j).getAge())
                        result.add(first.get(i));
                    else
                        result.add(second.get(j));
                    found = true;
                    break;
                }
            }
            if(!found) result.add(first.get(i));
        }

        for(int i=0; i< second.size(); i++) {
            boolean found = false;
            for(int j=0; j<first.size(); j++) {
                if(second.get(i).getPeerAddress().equals(first.get(j).getPeerAddress())) {
                    found = true;
                    break;
                }
            }
            if(!found) result.add(second.get(i));
        }

        return result;
    }
}
