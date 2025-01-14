package net.bhl.matsim.uam.analysis.trips.run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.Vehicle;

import net.bhl.matsim.uam.analysis.trips.DeckGLTripItem;
import net.bhl.matsim.uam.analysis.trips.listeners.DeckGLTripListener;

public class ConvertDeckGLTripsFromEvents {
	
	private static String inCRS = "EPSG:2154";
	private static String outCRS = "EPSG:4326";
	private static long deckGLanimationSpeed = 1;
	private static long minTime = 21600;
	private static long maxTime = minTime + (1 * 3600);
	
	static public void main(String[] args) throws IOException {
		// PROVIDE: NETWORK EVENTS INPUT_CRS*
		// * optional
		if (args.length == 3)
			inCRS = args[2];
		
		extract(args[0], args[1]);
		System.out.println("done.");
	}

	static public void extract(String network, String events) throws IOException {
		Network netw = NetworkUtils.createNetwork();
		new MatsimNetworkReader(netw).readFile(network);

		DeckGLTripListener deckGLTripListener = new DeckGLTripListener(netw, minTime, maxTime);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(deckGLTripListener);
		new MatsimEventsReader(eventsManager).readFile(events);

		Map<Id<Vehicle>, List<DeckGLTripItem>> deckGLTrips = deckGLTripListener.getDeckGLTripItems();

		BufferedWriter bw = null;
		try {
			File file = new File(events + ".json");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			
			bw.write("[" + System.lineSeparator());
			
			for (Iterator<Id<Vehicle>> it = deckGLTrips.keySet().iterator(); it.hasNext();) {
				Id<Vehicle> id = it.next();
				int vendor = id.toString().contains("uam") ? 1 : 0;
				bw.write("{\"vendor\": " + vendor + ", ");
				bw.write("\"segments\": [");
				
				for(Iterator<DeckGLTripItem> itemIter = deckGLTrips.get(id).iterator(); itemIter.hasNext();) {
					DeckGLTripItem item = itemIter.next();
					bw.write(item.convert(inCRS, outCRS, deckGLanimationSpeed));
					
					if (itemIter.hasNext())
						bw.write(",");
				}
				
				bw.write("]}");
			    
			    if (it.hasNext())
			    	bw.write("," + System.lineSeparator());
			}

			bw.write(System.lineSeparator() + "]");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}