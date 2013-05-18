package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

import java.math.BigInteger;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {
        {
            StochasticProcess process1 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(1, Operations.peerJoin(5), uniform(0, Integer.MAX_VALUE));

                }
            };
            StochasticProcess process2 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(4, Operations.peerJoin(5), uniform(0, Integer.MAX_VALUE));
                }
            };
            StochasticProcess process3 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(10, Operations.addIndexEntry(), uniform(0, Integer.MAX_VALUE));
                }
            };

            StochasticProcess failLeader = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(1, Operations.peerFail, uniform(1, 1));
                }
            };

            process1.startAt(1000);
            process2.startAfterTerminationOf(2000, process1);
            process3.startAfterTerminationOf(1000, process2);
            failLeader.startAfterTerminationOf(5000, process3);
        }
    };

//-------------------------------------------------------------------
    public Scenario1() {
        super(scenario);
    }
}
