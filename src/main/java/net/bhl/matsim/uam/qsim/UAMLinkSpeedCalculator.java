package net.bhl.matsim.uam.qsim;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class UAMLinkSpeedCalculator implements LinkSpeedCalculator {

	final private Map<String, Double> mapVehicleVerticalSpeeds;
	final private Map<String, Double> mapVehicleHorizontalSpeeds;

	final private LinkSpeedCalculator delegate;
	final private double crossingPenalty;

	public UAMLinkSpeedCalculator(Map<String, Double> mapVehicleVerticalSpeeds,
			Map<String, Double> mapVehicleHorizontalSpeeds, LinkSpeedCalculator delegate, double crossingPenalty) {
		this.mapVehicleVerticalSpeeds = mapVehicleVerticalSpeeds;
		this.mapVehicleHorizontalSpeeds = mapVehicleHorizontalSpeeds;

		this.delegate = delegate;
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {

		if (link.getId().toString().startsWith("uam_vl")) {
			return Math.min(link.getFreespeed(), this.mapVehicleVerticalSpeeds.get(vehicle.getId().toString()));

		}

		if (link.getId().toString().startsWith("uam_hl")) {
			return Math.min(link.getFreespeed(), this.mapVehicleHorizontalSpeeds.get(vehicle.getId().toString()));

		}

		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}

		if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return delegate.getMaximumVelocity(vehicle, link, time);
		} else {
			double travelTime = link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			travelTime += crossingPenalty;
			return link.getLength() / travelTime;
		}

	}

}
