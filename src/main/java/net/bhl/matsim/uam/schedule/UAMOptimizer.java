package net.bhl.matsim.uam.schedule;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.bhl.matsim.uam.dispatcher.Dispatcher;
import net.bhl.matsim.uam.infrastructure.UAMVehicle;
import net.bhl.matsim.uam.passenger.UAMRequest;

@Singleton
public class UAMOptimizer implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener {
	private double now;
	
	private Dispatcher dispatcher;
	@Inject
	private EventsManager eventsManager;

	@Override
	public void requestSubmitted(Request request) {
		UAMRequest uamRequest = (UAMRequest) request;
		dispatcher = uamRequest.getDispatcher();

		synchronized (dispatcher) {
			dispatcher.onRequestSubmitted(uamRequest);
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		//this happens at the start of the simulation since
		//the schedule has not started yet
		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			schedule.nextTask();
			return;
		}
		// STARTED
		// get the current task and make it end now
		Task currentTask = schedule.getCurrentTask();
		currentTask.setEndTime(now);

		List<? extends Task> tasks = schedule.getTasks();
		int index = currentTask.getTaskIdx() + 1;
		UAMTask nextTask = null;

		if (index < tasks.size()) {
			nextTask = (UAMTask) tasks.get(index);
		} else {
			throw new IllegalStateException("A UAM schedule should never end!");
		}

		double startTime = now;

		UAMTask indexTask;
		//we have to adapt the times of the rest of the tasks
		//in order to take into account any delays vehicle has experienced so far
		while (index < tasks.size()) {
			indexTask = (UAMTask) tasks.get(index);

			if (indexTask.getUAMTaskType() == UAMTask.UAMTaskType.STAY) {
				if (indexTask.getEndTime() < startTime)
					indexTask.setEndTime(startTime);
			} else {
				indexTask.setEndTime(indexTask.getEndTime() - indexTask.getBeginTime() + startTime);
			}

			indexTask.setBeginTime(startTime);
			startTime = indexTask.getEndTime();
			index++;
		}

		ensureNonFinishingSchedule(schedule);
		schedule.nextTask();
		ensureNonFinishingSchedule(schedule);

		// UAMDispatcher dispatcher = ((UAMVehicle) vehicle).getDispatcher();

		if (nextTask != null) {
			synchronized (dispatcher) {
				dispatcher.onNextTaskStarted((UAMVehicle) vehicle);
			}
		}

		if (nextTask != null && nextTask instanceof UAMDropoffTask) {
			// throws a transit event in order to let us know that the drop off
			// has been performed
			// this is used later in the analysis to know which person was
			// dropped off
			processTransitEvent((UAMDropoffTask) nextTask);
		}
	}

	private void ensureNonFinishingSchedule(Schedule schedule) {
		UAMTask lastTask = (UAMTask) Schedules.getLastTask(schedule);

		if (lastTask.getUAMTaskType() != UAMTask.UAMTaskType.STAY) {
			throw new IllegalStateException("A UAM schedule should always end with a STAY task");
		}

		if (!Double.isInfinite(lastTask.getEndTime())) {
			throw new IllegalStateException("A UAM schedule should always end at time Infinity");
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		now = e.getSimulationTime();
	}

	private void processTransitEvent(UAMDropoffTask task) {
		for (UAMRequest request : task.getRequests()) {
			eventsManager.processEvent(new UAMTransitEvent(request, now));
		}
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link link) {
	}
}
