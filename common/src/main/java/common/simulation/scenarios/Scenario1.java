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
                    raise(1, Operations.peerJoin(5), uniform(13));

                }
            };
            StochasticProcess process2 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(1000));
                    raise(5, Operations.peerJoin(5), uniform(13));
                }
            };
            StochasticProcess process3 = new StochasticProcess() {
                {
                    eventInterArrivalTime(constant(100));
                    raise(10, Operations.addIndexEntry(), uniform(13));
                }
            };
//
//            StochasticProcess process4 = new StochasticProcess() {
//                {
//                    raise(1, Operations.peerFail,
//                }
//            };

            process1.startAt(1000);
            process2.startAfterTerminationOf(2000, process1);
            process3.startAfterTerminationOf(2000, process2);
//            process4.startAt(15000);
        }
    };

//-------------------------------------------------------------------
    public Scenario1() {
        super(scenario);
    }
}
