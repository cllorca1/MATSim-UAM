package net.bhl.matsim.uam.modechoice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.TripStructureUtils.Trip;

import ch.ethz.matsim.mode_choice.framework.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.framework.tour_based.constraints.TourConstraint;
import ch.ethz.matsim.mode_choice.framework.tour_based.constraints.TourConstraintFactory;
import ch.ethz.matsim.mode_choice.framework.tour_based.estimation.TourCandidate;
import net.bhl.matsim.uam.modechoice.utils.VehicleLocationUtils;

public class VehicleTourConstraint implements TourConstraint {
	final private Collection<String> constrainedModes;
	final private List<ModeChoiceTrip> trips;
	final private Id<Link> homeLinkId;

	public VehicleTourConstraint(Id<Link> homeLinkId, Collection<String> constrainedModes, List<ModeChoiceTrip> trips) {
		this.homeLinkId = homeLinkId;
		this.trips = trips;
		this.constrainedModes = constrainedModes;
	}

	@Override
	public boolean validateBeforeEstimation(List<String> modes, List<List<String>> previousModes) {
		int tourOffset = previousModes.stream().mapToInt(Collection::size).sum();
		for (String testMode : constrainedModes) {
			int firstModeIndex = modes.indexOf(testMode);
			int lastModeIndex = modes.lastIndexOf(testMode);

			if (firstModeIndex > -1) { // The test mode is used at least once
				Trip firstTripInformation = trips.get(firstModeIndex + tourOffset).getTripInformation();
				Trip lastTripInformation = trips.get(lastModeIndex + tourOffset).getTripInformation();

				//Trip firstTripInformation = trips.get(firstModeIndex).getTripInformation();
				//Trip lastTripInformation = trips.get(lastModeIndex).getTripInformation();

				Id<Link> firstDepartureLinkId = VehicleLocationUtils.getOriginLinkId(firstTripInformation);
				Id<Link> lastArrivalLinkId = VehicleLocationUtils.getDestinationLinkId(lastTripInformation);

				if (!firstDepartureLinkId.equals(homeLinkId)) {
					// The first trip with this mode does not start at home
					return false;
				}

				if (!lastArrivalLinkId.equals(homeLinkId)) {
					// The last trip with this mode does not end at home
					return false;
				}

				Id<Link> currentLinkId = VehicleLocationUtils.getDestinationLinkId(firstTripInformation);

				for (int i = firstModeIndex + 1; i <= lastModeIndex; i++) {
					if (modes.get(i).equals(testMode)) {
						Trip tripInformation = trips.get(i + tourOffset).getTripInformation();

						if (!currentLinkId.equals(VehicleLocationUtils.getOriginLinkId(tripInformation))) {
							// Departure at one link, but car is at another link
							return false;
						}

						currentLinkId = VehicleLocationUtils.getDestinationLinkId(tripInformation);
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(TourCandidate candidate, List<TourCandidate> previousCandidates) {
		return true;
	}

	public static class Factory implements TourConstraintFactory {
		final private Collection<String> constrainedModes;

		public Factory(Collection<String> constrainedModes) {
			this.constrainedModes = constrainedModes;
		}

		@Override
		public TourConstraint createConstraint(List<ModeChoiceTrip> trips, Collection<String> availableModes) {
			Id<Link> homeLinkId = VehicleLocationUtils.getHomeLinkId(trips);
			return new VehicleTourConstraint(homeLinkId, constrainedModes, trips);
		}
	}
}
