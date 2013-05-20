package common.simulation;

import se.sics.kompics.Event;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: filippia
 * Date: 5/20/13
 * Time: 12:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeerFailLeader extends Event implements Serializable {

    private final Long id;

    //-------------------------------------------------------------------
    public PeerFailLeader(Long id) {
        this.id = id;
    }

    //-------------------------------------------------------------------
    public Long getId() {
        return id;
    }
}
