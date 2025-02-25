package net.bhl.matsim.uam.dispatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;

import net.bhl.matsim.uam.infrastructure.UAMStation;
import net.bhl.matsim.uam.infrastructure.UAMVehicle;
import net.bhl.matsim.uam.passenger.UAMRequest;
import net.bhl.matsim.uam.schedule.UAMDropoffTask;
import net.bhl.matsim.uam.schedule.UAMFlyTask;
import net.bhl.matsim.uam.schedule.UAMPickupTask;
import net.bhl.matsim.uam.schedule.UAMSingleRideAppender;
import net.bhl.matsim.uam.schedule.UAMStayTask;
import net.bhl.matsim.uam.schedule.UAMTask;

public class UAMPooledDispatcher implements Dispatcher {
	@Inject
	final private UAMSingleRideAppender appender;
	boolean reoptimize = true;
	final private Queue<UAMVehicle> availableVehicles = new LinkedList<>();
	final Set<UAMVehicle> enRouteToPickupVehicles = new HashSet<>();
	final private Queue<UAMRequest> pendingRequests = new LinkedList<>();
	final private QuadTree<UAMVehicle> availableVehiclesTree;
	private Map<UAMVehicle, Coord> locationVehicles = new HashMap<>();

	@Inject
	public UAMPooledDispatcher(UAMSingleRideAppender appender, UAMManager uamManager, Network network) {
		this.appender = appender;
		this.appender.setLandingStations(uamManager.getStations());
		
		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values()); // minx,

		availableVehiclesTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);

		for (Vehicle veh : uamManager.getVehicles().values()) {
			this.availableVehicles.add((UAMVehicle) veh);
			
			Id<UAMStation> stationId = ((UAMVehicle)veh).getInitialStationId();
			UAMStation uamStation = uamManager.getStations().getUAMStations().get(stationId);
			Link linkStation = uamStation.getLocationLink();
			Coord coord = linkStation.getCoord();
			
			this.availableVehiclesTree.put(coord.getX(), coord.getY(), (UAMVehicle)veh);
			locationVehicles.put((UAMVehicle)veh, coord);
		}
	}

	@Override
	public void onNextTimeStep(double now) {
		appender.update();
		if (reoptimize)
			reoptimize(now);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bhl.matsim.uam.dispatcher.Dispatcher#onRequestSubmitted(net.bhl.
	 * matsim.uam.passanger.UAMRequest)
	 */
	@Override
	public void onRequestSubmitted(UAMRequest request) {
		pendingRequests.add(request);
		reoptimize = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bhl.matsim.uam.dispatcher.Dispatcher#onNextTaskStarted(net.bhl.matsim
	 * .uam.infrastructure.UAMVehicle)
	 */
	@Override
	public void onNextTaskStarted(UAMVehicle vehicle) {
		UAMTask task = (UAMTask) vehicle.getSchedule().getCurrentTask();
		if (task.getUAMTaskType() == UAMTask.UAMTaskType.STAY) {
			availableVehicles.add(vehicle);
			Coord coord = ((UAMStayTask)task).getLink().getCoord();
			this.availableVehiclesTree.put(coord.getX(), coord.getY(), vehicle);
			this.locationVehicles.put(vehicle, coord);
			reoptimize = true;
		} else if (task.getUAMTaskType() == UAMTask.UAMTaskType.PICKUP)
			this.enRouteToPickupVehicles.remove(vehicle);

	}

	private void reoptimize(double now) {

		//TODO: have pending requests per station
		while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
			UAMRequest request = pendingRequests.poll();

			if (!findEligableEnRouteVehicle(request)) {
				UAMVehicle vehicle = this.availableVehiclesTree.getClosest(request.getFromLink().getCoord().getX(),
						request.getFromLink().getCoord().getY());
				Coord coord = this.locationVehicles.get(vehicle);
				this.availableVehiclesTree.remove(coord.getX(),
						coord.getY(), vehicle);
				this.availableVehicles.remove(vehicle);
				//UAMVehicle vehicle = availableVehicles.poll();
				appender.schedule(request, vehicle, now);
				if (vehicle.getCapacity() > 1)
					this.enRouteToPickupVehicles.add(vehicle);
			}
		}
		
		if (availableVehicles.size() == 0) {
			
			while (pendingRequests.size() > 0) {
				
				UAMRequest request = pendingRequests.peek();
				if (!findEligableEnRouteVehicle(request))
					break;
				else
					pendingRequests.remove();
			}
		}

		//reoptimize = false;
	}

	private boolean findEligableEnRouteVehicle(UAMRequest request) {

		for (UAMVehicle vehicle : enRouteToPickupVehicles) {

			Schedule schedule = vehicle.getSchedule();
			if (schedule.getCurrentTask() instanceof UAMFlyTask) {
				int index = schedule.getTasks().indexOf(schedule.getCurrentTask());
				
				if (schedule.getTasks().get(index + 1) instanceof UAMPickupTask) {
					UAMPickupTask pickupTask = (UAMPickupTask) schedule.getTasks().get(index + 1);
					UAMRequest oldReq = (UAMRequest) pickupTask.getRequests().toArray()[0];
					if (oldReq.getToLink() == request.getToLink() && oldReq.getFromLink() == request.getFromLink()) {
						request.setDistance(oldReq.getDistance());
						pickupTask.getRequests().add(request);
						UAMDropoffTask dropOff = (UAMDropoffTask) schedule.getTasks().get(index + 3);
						dropOff.getRequests().add(request);
						
						if ((int)vehicle.getCapacity() == dropOff.getRequests().size())
							this.enRouteToPickupVehicles.remove(vehicle);
						
						return true;
					}
				} else {
					Logger log = Logger.getLogger(UAMPooledDispatcher.class);
					log.warn("Task following a UAMFlyTask is unexpectedly not a UAMPickupTask for vehicle: " + vehicle.getId());
				}
			}
		}

		return false;
	}

}
