package net.bhl.matsim.uam.qsim;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;

public class UAMPassengerEngine extends PassengerEngine{

	public UAMPassengerEngine(String mode, EventsManager eventsManager, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, Network network) {
		super(mode, eventsManager, requestCreator, optimizer, network);
	}
	
	

}