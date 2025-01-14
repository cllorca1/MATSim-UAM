package net.bhl.matsim.uam.schedule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import net.bhl.matsim.uam.passenger.UAMRequest;

/** 
 *  This task represents the local pick up of passenger at stations, it doesn't include the flying period.
 */

public class UAMPickupTask extends StayTaskImpl implements UAMTask{
	private final Set<UAMRequest> requests = new HashSet<>();
	private final double boardingTime;

	public UAMPickupTask(double beginTime, double endTime, Link link, double boardingTime) {
        super(beginTime, endTime, link);
        this.boardingTime = boardingTime;
	}

    public UAMPickupTask(double beginTime, double endTime, Link link, double boardingTime, Collection<UAMRequest> requests) {
        this(beginTime, endTime, link, boardingTime);

        this.requests.addAll(requests);
        for (UAMRequest request : requests) 
        	request.setPickupTask(this);
    }

	@Override
	public UAMTaskType getUAMTaskType() {
		return UAMTaskType.PICKUP;
	}

	@Override
	public Set<UAMRequest> getRequests() {
		return this.requests;
	}

	@Override
	public void addRequest(UAMRequest request) {
		requests.add(request);
        request.setPickupTask(this);
		
	}

	public double getBoardingTime() {
		return boardingTime;
	}

}
