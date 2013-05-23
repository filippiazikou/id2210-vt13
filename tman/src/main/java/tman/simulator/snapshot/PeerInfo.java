package tman.simulator.snapshot;

import common.peer.PeerAddress;
import java.util.ArrayList;


public class PeerInfo {
    private ArrayList<PeerAddress> tmanPartners;
    private ArrayList<PeerAddress> cyclonPartners;
    private int convergedCycles = 0;
    private int beforeConvergeCycles = 0;



    //-------------------------------------------------------------------
    public PeerInfo() {
        this.tmanPartners = new ArrayList<PeerAddress>();
        this.cyclonPartners = new ArrayList<PeerAddress>();
    }

    public int getConvergedCycles() {
        return convergedCycles;
    }

    public void increaseConvergedCycles() {
        this.convergedCycles += 1;
    }

    public void initConvergedCycles() {
        this.convergedCycles = 0;
    }

    public int getBeforeConvergeCycles() {
        return beforeConvergeCycles;
    }

    public void increaseBeforeConvergeCycles() {
        beforeConvergeCycles +=1;
    }

    public void initBeforeConvergeCycles() {
        beforeConvergeCycles =0;
    }

    //-------------------------------------------------------------------
    public void updateTManPartners(ArrayList<PeerAddress> partners) {
        this.tmanPartners = partners;
    }

    //-------------------------------------------------------------------
    public void updateCyclonPartners(ArrayList<PeerAddress> partners) {
        this.cyclonPartners = partners;
    }

    //-------------------------------------------------------------------
    public ArrayList<PeerAddress> getTManPartners() {
        return this.tmanPartners;
    }

    //-------------------------------------------------------------------
    public ArrayList<PeerAddress> getCyclonPartners() {
        return this.cyclonPartners;
    }
}