package net.bhl.matsim.uam.schedule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import net.bhl.matsim.uam.passenger.UAMRequest;

/** 
 *  This task represents the local drop-off of passenger at stations, it doesn't include the flying period.
 */

public class UAMDropoffTask extends StayTaskImpl implements UAMTask {
	private final Set<UAMRequest> requests = new HashSet<>();
	private final double deboardingTime;

	public UAMDropoffTask(double beginTime, double endTime, Link link, double deboardingTime) {
		super(beginTime, endTime, link);
		this.deboardingTime = deboardingTime;
	}
	
	public UAMDropoffTask(double beginTime, double endTime, Link link, double deboardingTime, Collection<UAMRequest> requests) {
		super(beginTime, endTime, link);

		this.requests.addAll(requests);
		for (UAMRequest request : requests) request.setDropoffTask(this);
		
		this.deboardingTime = deboardingTime;
	}

	@Override
	public UAMTaskType getUAMTaskType() {
		return UAMTaskType.DROPOFF;
	}

	@Override
	public Set<UAMRequest> getRequests() {
		return this.requests;
	}

	@Override
	public void addRequest(UAMRequest request) {

		this.requests.add(request);
	}
	
	public double getDeboardingTime() {
		return deboardingTime;
	}

}
