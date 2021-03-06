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
import se.sics.kompics.*;
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
    private LeaderKnowledge leaderKnowledge = LeaderKnowledge.NO;
    private PeerAddress leader = null;
    boolean isWaitingForElectionMessage = false;
    private int T = 1000;
    private int cyclesCount = 0;
    private int cyclesForLeaderTest = 5;
    private int viewSize;
    private int sampleSize;
    private boolean pingFromLeader = false;
    private int requiredAcks = 0;
    private boolean isLeaderSuspected = false;
    private ArrayList<PeerAddress> arrayForGradientLeaders = new ArrayList<PeerAddress>();
    private int modPartition;
    private int numberOfPartitions;

    private boolean isLeaderElectionRunning = false;

    private ArrayList<PeerDescriptor> view = new ArrayList<PeerDescriptor>();
    private ArrayList<PeerDescriptor> buffer = new ArrayList<PeerDescriptor>();
    //list to understand when gradient is stable to start a leader election
    private ArrayList<PeerDescriptor> oldView = new ArrayList<PeerDescriptor>();
    //maintains a list of peers that send heartbeat message to the leader to start heartbeat from peers that join view on the leader
    private ArrayList<PeerDescriptor> pingingMe =new ArrayList<PeerDescriptor>();
    Random rnd = new Random();

    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

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
        subscribe(coordinatorMessageHandler, networkPort);
        subscribe(electionMessageHandler, networkPort);
        subscribe(okMessageHandler, networkPort);
        subscribe(electionMessageTimeoutHandler, timerPort);
        subscribe(electionMessageWaitingTimeoutHandler, timerPort);
        subscribe(pingScheduleHandler, timerPort);
        subscribe(pingResponseScheduleHandler, timerPort);
        subscribe(pingMessageHandler, networkPort);
        subscribe(pingOkMessageHandler, networkPort);
        subscribe(isLeaderSuspectedMessageHandler, networkPort);
        subscribe(isLeaderSuspectedResultMessageHandler, networkPort);
        subscribe(handleAddEntryRequestFromSearch, tmanPartnersPort);
        subscribe(handleAddEntryRequestFromTman, networkPort);
        subscribe(handleAddEntryACK, networkPort);
        subscribe(handleAddEntryACK, tmanPartnersPort);
    }
    //-------------------------------------------------------------------
    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
            self = init.getSelf();

            numberOfPartitions = init.getPartitionAmount();
            modPartition = self.getPeerAddress().getId() % numberOfPartitions;

            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();
            viewSize = init.getViewSize();
            sampleSize = init.getExcahngeSampleSize();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);

            ScheduleTimeout pingLeaderTimeout = new ScheduleTimeout(period);
            pingLeaderTimeout.setTimeoutEvent(new PingSchedule(pingLeaderTimeout));
            trigger(pingLeaderTimeout, timerPort);
        }
    };

    //------------------------ Gradient -----------------------------

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            tmanPartners = getPartners();
            Snapshot.updateTManPartners(self, tmanPartners);

            //Snapshot.updateTManPartners(self, tmanPartners);

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

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            ArrayList<PeerAddress> cyclonPartners = removePartnersNotFromYourPartiotion(event.getSample());

            if(cyclonPartners.size() == 0) return;

            //choose peer to exchange view with
            PeerDescriptor q = selectPeerFromView();
            buffer = merge(view, new ArrayList<PeerDescriptor>(){{new PeerDescriptor(self);}});

            PeerAddress rndPeer = cyclonPartners.get(rnd.nextInt(cyclonPartners.size()));
            ArrayList<PeerDescriptor> myDescriptor = new ArrayList<PeerDescriptor>();
            myDescriptor.add(new PeerDescriptor(rndPeer));
            //merge buffer with self
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

    private ArrayList<PeerAddress> removePartnersNotFromYourPartiotion(ArrayList<PeerAddress> cyclonPartners) {
        ArrayList<PeerAddress> result = new ArrayList<PeerAddress>();

        for(int i=0; i<cyclonPartners.size(); i++) {
            int partnerMod = cyclonPartners.get(i).getPeerAddress().getId() % numberOfPartitions;
            if(partnerMod == modPartition)
                result.add(cyclonPartners.get(i));
        }

        return result;
    }

    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            buffer = merge(view, new ArrayList<PeerDescriptor>(){{new PeerDescriptor(self);}});
            Collections.sort(buffer, new RankComparator(event.getPeerSource()));
            //send to the peer his closest neighbours
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

            //if peers in view are leaving, we need to keep pinging the leader from new peers
            if(leader != null && leader.equals(self)) {
                for(int i=0; i<view.size(); i++) {
                    if(!view.get(i).getPeerAddress().getPeerAddress().equals(self) && !pingingMe.contains(view.get(i))){
                        trigger(new CoordinatorMessage(self, view.get(i).getPeerAddress()), networkPort);
                        pingingMe.add(view.get(i));
                    }
                }
            }

//            logger.info("============= View =============");
//            for(int i=0; i<view.size(); i++) {
//                logger.info(String.format("%s partner - %s", self.getPeerAddress().getId(), view.get(i).getPeerAddress().getPeerAddress().getId()));
//            }
//            logger.info("++++++++++++++++++++++++++++++++++++");

            //Increase age for peers that did not appear to the peers of event and remove if > 100
            int age;
            for (int i = 0 ; i<buffer.size() ; i++) {
                if (!event.getSelectedBuffer().getDescriptors().contains(buffer.get(i)))  {
                    age = buffer.get(i).incrementAndGetAge();
                    if (age > 100)
                        buffer.remove(i);
                }
            }

            //peers with id <= partitionAmount will start leader election when gradient is stable
            TryToStartLeaderElection();
        }

        private void TryToStartLeaderElection() {
            if(self.getPeerAddress().getId() > numberOfPartitions || leader != null) return;

            cyclesCount++;
            //trying to understand is gradient stable...
            if(oldView.size() == 0) {
                oldView = view;
                return;
            }

            if(oldView.size() != view.size()) {
                oldView = view;
                cyclesForLeaderTest = 5;
                return;
            }

            boolean containsAll = true;
            for(int i=0; i<view.size(); i++){
                if(!oldView.contains(view.get(i))) {
                    containsAll = false;
                    break;
                }
            }

            if(!containsAll) {
                oldView = view;
                cyclesForLeaderTest = 5;
                return;
            }

            cyclesForLeaderTest--;
            if(cyclesForLeaderTest == 0) {
                logger.info(String.format("%s - Starting leader election on %s cycle, gradient converged on %s cycle", self.getPeerAddress().getId(), cyclesCount, cyclesCount - 5));
                startLeaderElection(null);
            }
        }
    };

    //take sampleSize amount of message to do view exchange
    private ArrayList<PeerDescriptor> takeM(ArrayList<PeerDescriptor> buffer) {
        if(buffer.size() <= sampleSize)
            return buffer;

        ArrayList<PeerDescriptor> result = new ArrayList<PeerDescriptor>();
        for(int i=0; i< sampleSize; i++)
            result.add(buffer.get(i));
        return result;
    }

    private ArrayList<PeerDescriptor> selectView() {
        PeerDescriptor own = null;
        //get rid of self when bufferSize = 3
        for(int i = 0; i < buffer.size(); i++) {
            if(buffer.size() < 3) break;
            if(buffer.get(i).getPeerAddress().getPeerAddress().equals(self.getPeerAddress())) {
                own = buffer.get(i);
                break;
            }
        }

        if(own != null)
            buffer.remove(own);

        //return buffer if is't less than viewSize
        if(buffer.size() <= viewSize)
            return buffer;

        ArrayList<PeerDescriptor> view = new ArrayList<PeerDescriptor>();
        for(int i=0; i< viewSize; i++)
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

        for(int i=0; i<first.size(); i++){
            boolean found = false;

            for(int j=0; j<second.size(); j++){
                //items with the same Address
                if(first.get(i).equals(second.get(j))) {
                    found = true;

                    //save item with a smaller age
                    if(first.get(i).getAge() <= second.get(j).getAge())
                        result.add(first.get(i));
                    else
                        result.add(second.get(j));

                    break;
                }
            }

            if(!found) result.add(first.get(i));
        }

        for(int i=0; i<second.size(); i++){
            if(!result.contains(second.get(i)))
                result.add(second.get(i));
        }

        return result;
    }

    //---------------------------------------------------------------

    //-------------------- Heartbeat --------------------------------
    Handler<PingSchedule> pingScheduleHandler = new Handler<PingSchedule>() {
    @Override
    public void handle(PingSchedule pingSchedule) {
        if(leader == null || leader.equals(self)) return;

        ScheduleTimeout pingResponseTimeout = new ScheduleTimeout(T);
        pingResponseTimeout.setTimeoutEvent(new PingResponseSchedule(pingResponseTimeout));
        pingFromLeader = false;
        trigger(new PingMessage(self, leader), networkPort);
        trigger(pingResponseTimeout, timerPort);

        //logger.info(String.format("%s ping to %s", self.getPeerAddress().getId(), leader.getPeerAddress().getId()));
        }
    };

    Handler<PingMessage> pingMessageHandler = new Handler<PingMessage>() {
        @Override
        public void handle(PingMessage pingMessage) {
            trigger(new PingOkMessage(self, pingMessage.getPeerSource()), networkPort);
        }
    };

    Handler<PingOkMessage> pingOkMessageHandler = new Handler<PingOkMessage>() {
        @Override
        public void handle(PingOkMessage pingOkMessage) {
            isLeaderSuspected = false;
            pingFromLeader = true;
        }
    };

    Handler<PingResponseSchedule> pingResponseScheduleHandler = new Handler<PingResponseSchedule>() {
    @Override
    public void handle(PingResponseSchedule pingResponseSchedule) {
        if(leader!=null && pingFromLeader) {
            ScheduleTimeout pingResponseTimeout = new ScheduleTimeout(T);
            pingResponseTimeout.setTimeoutEvent(new PingResponseSchedule(pingResponseTimeout));
            pingFromLeader = false;
            trigger(new PingMessage(self, leader), networkPort);
            trigger(pingResponseTimeout, timerPort);
            return;
        }

        //no ping - ask neighbours, if majority of then agree, start leader election
        isLeaderSuspected = true;
        logger.info(String.format("%s - leader suspected", self.getPeerAddress().getId()));
        requiredAcks = (int)Math.ceil(((double)(view.size()-1))/2);

        logger.info("============= View =============");
        for(int i=0; i<view.size(); i++) {
            logger.info(String.format("%s partner - %s age %s", self.getPeerAddress().getId(), view.get(i).getPeerAddress().getPeerAddress().getId(), view.get(i).getAge()));
        }
        logger.info("++++++++++++++++++++++++++++++++++++");

        logger.info(String.format("Need %s acks", requiredAcks));
            for(int i=0; i<view.size(); i++)
                trigger(new IsLeaderSuspectedMessage(self, view.get(i).getPeerAddress()), networkPort);
        }
    };

    Handler<IsLeaderSuspectedMessage> isLeaderSuspectedMessageHandler = new Handler<IsLeaderSuspectedMessage>() {
        @Override
        public void handle(IsLeaderSuspectedMessage isLeaderSuspectedMessage) {
            if(leader == null) {
                trigger(new IsLeaderSuspectedResultMessage(self, isLeaderSuspectedMessage.getPeerSource(), true), networkPort);
                return;
            }
            trigger(new IsLeaderSuspectedResultMessage(self, isLeaderSuspectedMessage.getPeerSource(), isLeaderSuspected), networkPort);
        }
    };

    Handler<IsLeaderSuspectedResultMessage> isLeaderSuspectedResultMessageHandler = new Handler<IsLeaderSuspectedResultMessage>() {
        @Override
        public void handle(IsLeaderSuspectedResultMessage isLeaderSuspectedResultMessage) {
            if(!isLeaderSuspectedResultMessage.isLeaderSuspected())
                return;

            requiredAcks--;
            if(requiredAcks != 0) return;

            logger.info(String.format("%s - got acks!", self.getPeerAddress().getId()));

            startLeaderElection(null);
        }
    };

    private void removeLeaderFromViewAndBuffer(PeerAddress leader) {
        for(int i=0; i<view.size(); i++)
            if(view.get(i).getPeerAddress().getPeerAddress().equals(leader.getPeerAddress()))
                view.remove(i);
        for(int i=0; i<buffer.size(); i++)
            if(buffer.get(i).getPeerAddress().getPeerAddress().equals(leader.getPeerAddress()))
                buffer.remove(i);
    }
    //-------------------------------------------------------------------

    //---------------------- Leader election ----------------------------
    private void startLeaderElection(ArrayList<PeerAddress> additionalPeer) {
        //Snapshot.leaderElectionStart(view.size());

        //know the leader
        if(leader != null && leader.equals(self)) {
            if(additionalPeer == null) return;

            //if another peer started leader election send him Election message
            for(PeerAddress peer : additionalPeer)
                trigger(new ElectionMessage(self, peer), networkPort);
            return;
        }

        if(isLeaderElectionRunning) return;

        if(leader != null) {
            removeLeaderFromViewAndBuffer(leader);
            leader = null;
        }

        logger.info("============= View =============");
        for(int i=0; i<view.size(); i++) {
            logger.info(String.format("%s partner - %s age %s", self.getPeerAddress().getId(), view.get(i).getPeerAddress().getPeerAddress().getId(), view.get(i).getAge()));
        }
        logger.info("++++++++++++++++++++++++++++++++++++");

        logger.info(String.format("%s - starting leader election", self.getPeerAddress().getId()));

        Collections.sort(view, new UtilityRanking());

        //if there is no peer with higher utility then leader = me
        if(view.size() > 0 && view.get(0).getPeerAddress().getPeerAddress().getId() >= self.getPeerAddress().getId()) {
            leaderKnowledge = LeaderKnowledge.I_M_THE_LEADER;
            leader = self;
            logger.info(self.getPeerAddress().getId() + " I'm the leader!");
            logger.info(String.format("%s - Leader: %s", self.getPeerAddress().getId(), leader.getPeerAddress().getId()));

            for(int i=0; i < view.size(); i++) {
                trigger(new CoordinatorMessage(self, view.get(i).getPeerAddress()), networkPort);
            }
            pingingMe = view;
            return;
        }

        //send election message to peers with higher utility then mine
        leaderKnowledge = LeaderKnowledge.MAYBE_ME;
        isLeaderElectionRunning = true;
        for(int i=0; i<view.size(); i++) {
            if(view.get(i).getPeerAddress().getPeerAddress().getId() >= self.getPeerAddress().getId())
                break;

            trigger(new ElectionMessage(self, view.get(i).getPeerAddress()), networkPort);
        }

        //if this election invoked by a node with lower utility, send an election message to it
        if(additionalPeer != null) {
            for(int i=0; i< additionalPeer.size(); i++) {
                trigger(new ElectionMessage(self, additionalPeer.get(i)), networkPort);
                arrayForGradientLeaders.remove(additionalPeer.get(i));
            }
        }

        ScheduleTimeout rst = new ScheduleTimeout(T);
        rst.setTimeoutEvent(new ElectionMessageTimeout(rst));
        trigger(rst, timerPort);
    }

    //Handle coordinator message that announces a leader
    Handler<CoordinatorMessage> coordinatorMessageHandler = new Handler<CoordinatorMessage>() {
        @Override
        public void handle(CoordinatorMessage coordinatorMessage) {
            leader = coordinatorMessage.getPeerSource();

//            Snapshot.increaseLeaderMessages();
//            Snapshot.leaderElectionTime();

            logger.info(String.format("%s - Leader: %s", self.getPeerAddress().getId(), leader.getPeerAddress().getId()));

            ScheduleTimeout pingLeaderTimeout = new ScheduleTimeout(period);
            pingLeaderTimeout.setTimeoutEvent(new PingSchedule(pingLeaderTimeout));
            trigger(pingLeaderTimeout, timerPort);
            return;
        }
    };

    //if no ok message from a node with higher utility after timeout, then I'm the leader
    Handler<ElectionMessageTimeout> electionMessageTimeoutHandler = new Handler<ElectionMessageTimeout>() {
        @Override
        public void handle(ElectionMessageTimeout electionMessageTimeout) {
            if(leaderKnowledge == LeaderKnowledge.MAYBE_ME) {
                leaderKnowledge = LeaderKnowledge.I_M_THE_LEADER;
                leader = self;
                for(int i=0; i < view.size(); i++)
                    trigger(new CoordinatorMessage(self, view.get(i).getPeerAddress()), networkPort);

                pingingMe = view;
                isLeaderElectionRunning = false;
                return;
            }

        }
    };

    Handler<ElectionMessage> electionMessageHandler = new Handler<ElectionMessage>() {
        @Override
        public void handle(ElectionMessage electionMessage) {
            //Snapshot.increaseLeaderMessages();

            logger.info(self.getPeerAddress().getId() + " see election message from " + electionMessage.getPeerSource().getPeerAddress().getId());
            trigger(new OkMessage(self, electionMessage.getPeerSource()), networkPort);

            if(isWaitingForElectionMessage) {
                isWaitingForElectionMessage = false;
                return;
            }

            //I have a bigger utility => start leader election and send Election message to neighbours and a peers that started leader election before
            if(self.getPeerAddress().getId() < electionMessage.getPeerSource().getPeerAddress().getId()) {
                arrayForGradientLeaders.add(electionMessage.getPeerSource());
                startLeaderElection(arrayForGradientLeaders);
            }
        }
    };

    //parse ok message from a node with higher utility
    Handler<OkMessage> okMessageHandler = new Handler<OkMessage>() {
        @Override
        public void handle(OkMessage okMessage) {
            if(okMessage.getPeerSource().getPeerAddress().getId() < self.getPeerAddress().getId()) {
                leaderKnowledge = LeaderKnowledge.NO;
                isWaitingForElectionMessage = true;

                ScheduleTimeout rst = new ScheduleTimeout(T);
                rst.setTimeoutEvent(new ElectionMessageWaitingTimeout(rst));
                trigger(rst, timerPort);
            }
        }
    };

    //start election again if failed to receive Election message after T
    Handler<ElectionMessageWaitingTimeout> electionMessageWaitingTimeoutHandler = new Handler<ElectionMessageWaitingTimeout>() {
        @Override
        public void handle(ElectionMessageWaitingTimeout electionMessageWaitingTimeout) {
            if(isWaitingForElectionMessage && leaderKnowledge == LeaderKnowledge.NO) {
                isWaitingForElectionMessage = false;
                leaderKnowledge = LeaderKnowledge.MAYBE_ME;
                startLeaderElection(null);
            }

        }
    };
    //---------------------------------------------------------------

    //---------------- Forward write to the leader ------------------

    //Handle AddIndexRequest triggered from Search Component
    Handler<AddEntryRequest> handleAddEntryRequestFromSearch = new Handler<AddEntryRequest>() {
        @Override
        public void handle(AddEntryRequest event) {
            addEntryRequest(event);
        }
    };

    //Handle AddIndexRequest  triggered from Tman
    Handler<AddEntryRequest> handleAddEntryRequestFromTman = new Handler<AddEntryRequest>() {
        @Override
        public void handle(AddEntryRequest event) {
            addEntryRequest(event);
        }
    };

    void addEntryRequest(AddEntryRequest event) {
        //if (event.getPeerSource() == event.getPeerDestination()) {
        //    Snapshot.initSteps();
        //}
        //else if (leader != null && leader.equals(self)) {
        //    Snapshot.finishSteps();
        //}
        //else {
        //    Snapshot.increaseSteps();
        //}

        //If I am the leader, trigger the request to search component
        if(leader != null && leader.equals(self)) {
            trigger(new AddEntryRequest(self, self, event.getInitiator(), event.getTitle(), event.getMagnet(), event.getRequestID()), tmanPartnersPort);
            return;
        }

        if(view.size() == 0) return;
        //if we still have ids lower than new leader, remove it
        if(leader != null) {
            for(int i=0; i<view.size(); i++)
                if(view.get(i).getPeerAddress().getPeerAddress().getId() < leader.getPeerAddress().getId())
                    view.remove(i);
        }

        //get the minimum id of view
        Collections.sort(view, new LinearComparator());
        //If I don't know the leader forward the request
        trigger(new AddEntryRequest(self, view.get(0).getPeerAddress(), event.getInitiator(), event.getTitle(), event.getMagnet(), event.getRequestID()), networkPort);
    }

    private PeerDescriptor getTheClosestToInitiator(PeerAddress initiator) {
        ArrayList<PeerDescriptor> sortedView = view;

        if(sortedView.isEmpty()) return null;

        Collections.sort(sortedView, new RankComparator(initiator));
        return sortedView.get(0);
    }

    //handles ACKs, adds index entry to self if get's ACK
    Handler<AddEntryACK> handleAddEntryACK = new Handler<AddEntryACK>() {
        @Override
        public void handle(AddEntryACK event) {
            if (event.getInitiator() == self)
                trigger(new AddEntryACK(self, self, self, event.getRequestID(), event.getEntryId()), tmanPartnersPort);
            else {
                PeerDescriptor closest = getTheClosestToInitiator(event.getInitiator());
                if(closest != null)
                    trigger(new AddEntryACK(self, closest.getPeerAddress(), event.getInitiator(), event.getRequestID(), event.getEntryId()), networkPort);
            }

            //if (leader!=null && leader.equals(self)) {
            //    Snapshot.initAck();
            //}
            //else if (event.getInitiator().equals(self)) {
            //    Snapshot.finishedAck();
            //}
            //else {
            //    Snapshot.increaseAck();
            //}
        }
    };
    //------------------------------------------------------------
}