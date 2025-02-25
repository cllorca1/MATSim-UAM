package net.bhl.matsim.uam.router.strategy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

import net.bhl.matsim.uam.data.UAMRoute;
import net.bhl.matsim.uam.events.UAMUtilitiesAccessEgress;
import net.bhl.matsim.uam.events.UAMUtilitiesData;
import net.bhl.matsim.uam.infrastructure.UAMStation;


public class UAMMaxAccessUtilityStrategy implements UAMStrategy{
	private UAMStrategyUtils strategyUtils;

	public UAMMaxAccessUtilityStrategy(UAMStrategyUtils strategyUtils) {
		this.strategyUtils = strategyUtils;
	}

	@Override
	public UAMStrategyType getUAMStrategyType() {
		return UAMStrategyType.MAXACCESSUTILITY;
	}

	@Override
	public UAMRoute getRoute(Person person, Facility<?> fromFacility, Facility<?> toFacility, double departureTime) {
		Network network = strategyUtils.getNetwork();
		Set<String> modes = new HashSet<>();
		modes = strategyUtils.getModes();
		Collection<UAMStation> stationsOrigin = strategyUtils.getPossibleStations(fromFacility);
		Collection<UAMStation> stationsDestination = strategyUtils.getPossibleStations(toFacility);		
		//Access utility
		UAMStation bestStationOrigin = null;
		String bestModeAccess = TransportMode.walk;
		double bestUtilityAccess = Double.NEGATIVE_INFINITY;
		for (UAMStation stationOrigin : stationsOrigin) {
			for (String mode : modes) {
				double accessUtility = strategyUtils.estimateUtilityWrapper(person, true, fromFacility, departureTime, stationOrigin, mode);
				if (accessUtility > bestUtilityAccess) {
					bestUtilityAccess = accessUtility;
					bestModeAccess = mode;
					bestStationOrigin = stationOrigin;
				}

				if (strategyUtils.getParameters().storeUAMUtilities != 0) {
					UAMUtilitiesData.accessEgressOptions.add(new UAMUtilitiesAccessEgress(person.getId(),
							stationOrigin.getId(), network.getLinks().get(fromFacility.getLinkId()).getId(), true, accessUtility, mode, departureTime));
				}
			}
		}
		//Egress utility
		UAMStation bestStationDestination = null;
		String bestModeEgress = TransportMode.walk;
		double bestUtilityEgress = Double.NEGATIVE_INFINITY;
		double timeOfEgress = departureTime;
		for (UAMStation stationDestination : stationsDestination) {
			if (bestStationOrigin == stationDestination)
				continue;
			double flyTime = strategyUtils.getFlyTime(bestStationOrigin, stationDestination);
			//updated departureTime 				
			double currentDepartureTime = departureTime + strategyUtils.estimateTime(true, fromFacility, departureTime, bestStationOrigin, bestModeAccess)+flyTime;
			for (String mode : modes) {
				double egressUtility = strategyUtils.estimateUtilityWrapper(person, false, toFacility, currentDepartureTime, stationDestination, mode);
				if (egressUtility > bestUtilityEgress) {
					bestUtilityEgress = egressUtility;
					bestModeEgress = mode;
					bestStationDestination = stationDestination;
					timeOfEgress = currentDepartureTime;
				}

				if (strategyUtils.getParameters().storeUAMUtilities != 0) {
					UAMUtilitiesData.accessEgressOptions.add(new UAMUtilitiesAccessEgress(person.getId(),
							stationDestination.getId(), network.getLinks().get(fromFacility.getLinkId()).getId(), false, egressUtility, mode, departureTime));
				}
			}
		}		
		double accessDistance = strategyUtils.estimateDistance(true, fromFacility, departureTime, bestStationOrigin, bestModeAccess);
		double egressDistance = strategyUtils.estimateDistance(false, toFacility, timeOfEgress, bestStationDestination,bestModeEgress);
		//if the access/egress distance is less than walkDistance, then walk will be the uam access and egress mode
		bestModeAccess = strategyUtils.checkStationAccessDistance(true, bestModeAccess, bestStationOrigin, null,
				accessDistance, fromFacility, toFacility, departureTime, null, false);
		bestModeEgress = strategyUtils.checkStationAccessDistance(false, bestModeEgress, bestStationDestination, bestStationOrigin,
				egressDistance, toFacility, fromFacility, departureTime, bestModeAccess, false);

		return new UAMRoute(bestModeAccess, bestStationOrigin, bestStationDestination, bestModeEgress);
	}

}
