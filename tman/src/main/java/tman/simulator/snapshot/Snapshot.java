package tman.simulator.snapshot;

import common.peer.PeerAddress;
import java.util.*;


public class Snapshot {
    private static TreeMap<PeerAddress, PeerInfo> peers = new TreeMap<PeerAddress, PeerInfo>();
    private static int counter = 0;
    private static String FILENAME = "tman.out";

    private static int leaderCounter = 0;
    private static int leaderAcks = -1;
    private static int leaderMessages = 0;
    private static boolean printedConverge = false;

    private static ArrayList<PeerAddress> convergedPeers = new ArrayList<PeerAddress>();

    private static long peersConvergedTime = 0;
    private static long firstPeerInsertedTime = 0;
    private static long lastPeerInsertedTime = 0;


    private static long leaderElectionStartedTime = 0;
    private static long leaderElectionFinishedTime = 0;


    private static int ackSteps = 0;
    private static int leaderSteps = 0;


    //-------------------------------------------------------------------
    public static void init(int numOfStripes) {
        FileIO.write("", FILENAME);
    }

    //-------------------------------------------------------------------
    public static void addPeer(PeerAddress address) {
        peers.put(address, new PeerInfo());
    }

    //-------------------------------------------------------------------
    public static void removePeer(PeerAddress address) {
        peers.remove(address);
    }

    //-------------------------------------------------------------------
    public static void updateTManPartners(PeerAddress address, ArrayList<PeerAddress> partners) {
        PeerInfo peerInfo = peers.get(address);


        if (peerInfo == null)  {
            peers.put(address, new PeerInfo());
            peerInfo = peers.get(address);
            lastPeerInsertedTime = System.currentTimeMillis();
            if (firstPeerInsertedTime == 0)
                firstPeerInsertedTime = System.currentTimeMillis();
        }
        Collections.sort(partners);
        Collections.sort(peers.get(address).getTManPartners());
        if (partners.equals(peers.get(address).getTManPartners())) {
            peers.get(address).increaseConvergedCycles();
        }
        else {
            peers.get(address).initConvergedCycles();
        }
        if (peers.get(address).getConvergedCycles() >= 5 && !convergedPeers.contains(address)){
            convergedPeers.add(address);
        }
        else {
            peers.get(address).increaseBeforeConvergeCycles();
        }
        if (convergedPeers.size() == peers.size() && printedConverge == false) {
            peersConvergedTime = System.currentTimeMillis();
             /*Print to file*/
            String str = new String();
            str += "Tman converged for "+peers.size()+" peers! It took "+(lastPeerInsertedTime-firstPeerInsertedTime)+" ms for all peers to be inserted, "+(peersConvergedTime-lastPeerInsertedTime)+" ms to converge from the time that the last peer inserted\n" ;
            for (PeerAddress peer : peers.keySet()) {
                str += "peer "+peer.getPeerAddress().getId()+" needed "+peers.get(peer).getConvergedCycles()+" cycles to converge\n";
            }
            str += "###\n";
            FileIO.append(str, FILENAME);
            printedConverge = true;
        }
        peerInfo.updateTManPartners(partners);
    }

    //-------------------------------------------------------------------
    public static void updateCyclonPartners(PeerAddress address, ArrayList<PeerAddress> partners) {
        PeerInfo peerInfo = peers.get(address);

        if (peerInfo == null)
            return;

        peerInfo.updateCyclonPartners(partners);
    }

    //-------------------------------------------------------------------
    public static void increaseLeaderMessages() {
        leaderMessages += 1;
    }

    //-------------------------------------------------------------------
    public static void initAck() {
        ackSteps = 0;
    }

    //-------------------------------------------------------------------
    public static void increaseAck() {
        ackSteps += 1;
    }
    //-------------------------------------------------------------------
    public static void finishedAck() {
        System.out.println("steps to get ack: "+ackSteps);
    }
    //-------------------------------------------------------------------
    public static void increaseSteps() {
        leaderSteps += 1;
    }
    //-------------------------------------------------------------------
    public static void initSteps() {
        leaderSteps = 0;
    }
    //-------------------------------------------------------------------
    public static void finishSteps() {
        System.out.println("steps to leader: "+leaderSteps);
    }
    //-------------------------------------------------------------------
//-------------------------------------------------------------------
    public static void report() {
        PeerAddress[] peersList = new PeerAddress[peers.size()];
        peers.keySet().toArray(peersList);

        String str = new String();
        str += "current time: " + counter++ + "\n";
        str += reportNetworkState();
        str += reportDetails();
        str += "###\n";

        System.out.println(str);
        FileIO.append(str, FILENAME);
    }

    //-------------------------------------------------------------------
    public static void leaderElectionTime() {
        leaderCounter += 1;
        if (leaderCounter == leaderAcks) {
            leaderElectionFinishedTime = System.currentTimeMillis();
            String str = new String();
            str +="Leader received all Acks after "+(leaderElectionFinishedTime-leaderElectionStartedTime) + " ms and needed "+leaderMessages+" messages\n";
            str += "###\n";
            FileIO.append(str, FILENAME);
        }
    }

    //-------------------------------------------------------------------
    public static void leaderElectionStart(int n) {
        leaderAcks = n;
        leaderElectionStartedTime =  System.currentTimeMillis();

        int min = -1;
        PeerAddress minAddress = null;
        for (PeerAddress peer : peers.keySet()) {
            if (min == -1) {
                min =peer.getPeerAddress().getId();
                minAddress = peer;
            }
            else if (peer.getPeerAddress().getId() < min) {
                min =  peer.getPeerAddress().getId();
                minAddress = peer;
            }
        }

        if (minAddress!=null) {
            String str = new String();
            str +="Leader election started at cycle: "+ peers.get(minAddress).getConvergedCycles()+"of first node";
            str += "###\n";
            FileIO.append(str, FILENAME);
        }
    }


    //-------------------------------------------------------------------
    private static String reportNetworkState() {
        String str = new String("---\n");
        int totalNumOfPeers = peers.size() - 1;
        str += "total number of peers: " + totalNumOfPeers + "\n";

        return str;
    }

    //-------------------------------------------------------------------
    private static String reportDetails() {
        PeerInfo peerInfo;
        String str = new String("---\n");

        for (PeerAddress peer : peers.keySet()) {
            peerInfo = peers.get(peer);

            str += "peer: " + peer;
            str += ", cyclon parters: " + peerInfo.getCyclonPartners();
            str += ", tman parters: " + peerInfo.getTManPartners();
            str += "\n";
        }

        return str;
    }


}