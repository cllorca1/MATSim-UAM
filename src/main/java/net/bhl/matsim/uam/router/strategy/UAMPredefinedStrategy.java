package net.bhl.matsim.uam.router.strategy;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.Facility;
import net.bhl.matsim.uam.data.UAMRoute;
import net.bhl.matsim.uam.infrastructure.UAMStation;
import net.bhl.matsim.uam.router.UAMIntermodalRoutingModule;

public class UAMPredefinedStrategy implements UAMStrategy{
	private UAMStrategyUtils strategyUtils;
	private static final Logger log = Logger.getLogger(UAMPredefinedStrategy.class);
	
	public static final String ACCESS_MODE = "accessMode";
	public static final String ORIG_STATION = "originStation";
	public static final String DEST_STATION = "destinationStation";
	public static final String EGRESS_MODE = "egressMode";

	public UAMPredefinedStrategy(UAMStrategyUtils strategyUtils) {
		this.strategyUtils = strategyUtils;
	}

	@Override
	public UAMStrategyType getUAMStrategyType() {
		return UAMStrategyType.PREDEFINED;
	}

	@Override
	public UAMRoute getRoute(Person person, Facility<?> fromFacility, Facility<?> toFacility, double departureTime) {
		Leg l = null;		
		List<PlanElement> elements = person.getSelectedPlan().getPlanElements();
		for (int i = 0; i < elements.size() - 2; i += 2) {
			Activity endingActivity = (Activity) elements.get(i);
			
			if (endingActivity.getType().equals(UAMIntermodalRoutingModule.UAM_INTERACTION))
				continue;
					
			if (endingActivity.getEndTime() == departureTime) {
				l = (Leg) elements.get(i + 1);
				break;
			}
		}

		if (l == null)
			reportMissingLeg(person);

		String accessMode = (String) l.getAttributes().getAttribute(ACCESS_MODE);
		String egressMode = (String) l.getAttributes().getAttribute(EGRESS_MODE);

		if (accessMode == null || egressMode == null)
			reportMissingMode(person);
		
		String definedOriginStation = (String) l.getAttributes().getAttribute(ORIG_STATION);
		String definedDestinationStation = (String) l.getAttributes().getAttribute(DEST_STATION);
		UAMStation originStation = null, destinationStation = null;
		Collection<UAMStation> originStations = strategyUtils.getPossibleStations(fromFacility);
		for (UAMStation station : originStations) {
			if (station.getId().toString().equals(definedOriginStation)) {
				originStation = station;
				break;
			}
		}
		
		Collection<UAMStation> destinationStations = strategyUtils.getPossibleStations(toFacility);
		for (UAMStation station : destinationStations) {
			if (station.getId().toString().equals(definedDestinationStation)) {
				destinationStation = station;
				break;
			}
		}
		
		if (originStation == null)
			reportMissingStation(person, definedOriginStation, originStations);
		
		if (destinationStation == null)
			reportMissingStation(person, definedDestinationStation, destinationStations);

		return new UAMRoute(accessMode, originStation, destinationStation, egressMode);		
	}

	private void reportMissingLeg(Person person) {
		log.warn("For person " + person.getId().toString() +
				", predefined plan could not be parsed as UAM trip.");
		throw new NullPointerException();
	}

	private void reportMissingMode(Person person) {
		log.warn("For person " + person.getId().toString() +
				", predefined access and/or egress mode could not be found.");
		throw new NullPointerException();
	}

	private void reportMissingStation(Person person, String station, Collection<UAMStation> stations) {
		StringBuilder warning = new StringBuilder("For person " + person.getId().toString() +
				", predefined stations could not be found: " + station + " within possible stations: ");

		for (UAMStation st : stations)
			warning.append(st.getId().toString()).append(" ");
		warning.append(".");

		log.warn(warning);
		throw new NullPointerException();
	}

}
