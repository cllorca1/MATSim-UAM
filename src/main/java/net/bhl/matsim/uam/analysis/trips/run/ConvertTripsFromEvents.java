package net.bhl.matsim.uam.analysis.trips.run;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import net.bhl.matsim.uam.analysis.trips.CSVTripWriter;
import net.bhl.matsim.uam.analysis.trips.TripItem;
import net.bhl.matsim.uam.analysis.trips.listeners.TripListener;
import net.bhl.matsim.uam.analysis.trips.readers.EventsTripReader;
import net.bhl.matsim.uam.analysis.trips.utils.BasicHomeActivityTypes;
import net.bhl.matsim.uam.analysis.trips.utils.HomeActivityTypes;
import net.bhl.matsim.uam.router.UAMIntermodalRoutingModule;
import net.bhl.matsim.uam.router.UAMMainModeIdentifier;

public class ConvertTripsFromEvents {
	static public void main(String[] args) throws IOException {
		// PROVIDE: NETWORK EVENTS OUTFILE-NAME
		extract(args[0], args[1], args[2]);
		System.out.println("done.");
	}

	static public void extract(String network, String events, String outfile) throws IOException {
		Network netw = NetworkUtils.createNetwork();
		new MatsimNetworkReader(netw).readFile(network);

		// Add UAM stage activity types
		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE,
				UAMIntermodalRoutingModule.UAM_INTERACTION);

		HomeActivityTypes homeActivityTypes = new BasicHomeActivityTypes();
		MainModeIdentifier mainModeIdentifier = new UAMMainModeIdentifier(new MainModeIdentifierImpl());
		Collection<String> networkRouteModes = Arrays.asList("car", "uam", "access_uam_car", "egress_uam_car"); // Add
																												// uam
																												// (all
																												// network
																												// modes)

		TripListener tripListener = new TripListener(netw, stageActivityTypes, homeActivityTypes, mainModeIdentifier,
				networkRouteModes);
		Collection<TripItem> trips = new EventsTripReader(tripListener).readTrips(events);

		new CSVTripWriter(trips).write(outfile);
	}
}
