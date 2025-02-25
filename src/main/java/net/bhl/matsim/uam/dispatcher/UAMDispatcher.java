package net.bhl.matsim.uam.dispatcher;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.contrib.dvrp.data.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.bhl.matsim.uam.infrastructure.UAMVehicle;
import net.bhl.matsim.uam.passenger.UAMRequest;
import net.bhl.matsim.uam.schedule.UAMSingleRideAppender;
import net.bhl.matsim.uam.schedule.UAMTask;

@Singleton
public class UAMDispatcher implements Dispatcher {

	@Inject
	final private UAMSingleRideAppender appender;

	final private Queue<UAMVehicle> availableVehicles = new LinkedList<>();
	final private Queue<UAMRequest> pendingRequests = new LinkedList<>();
	private boolean reoptimize = false;

	@Inject
	public UAMDispatcher(UAMSingleRideAppender appender, UAMManager uamManager) {
		this.appender = appender;
		this.appender.setLandingStations(uamManager.getStations());

		for (Vehicle veh : uamManager.getVehicles().values()) {
			this.availableVehicles.add((UAMVehicle) veh);
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
		}
	}

	/**
	 * 
	 * @param now
	 *            current time
	 * 
	 *            Method that dispatches a first vehicle in the Queue - no
	 *            optimization.
	 */
	private void reoptimize(double now) {

		while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
			UAMVehicle vehicle = availableVehicles.poll();
			UAMRequest request = pendingRequests.poll();
			appender.schedule(request, vehicle, now);
		}

		reoptimize = false;
	}

}
