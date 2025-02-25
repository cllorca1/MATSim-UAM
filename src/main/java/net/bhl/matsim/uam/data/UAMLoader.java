package net.bhl.matsim.uam.data;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.bhl.matsim.uam.schedule.UAMStayTask;
/**
 * Class that loads the UAM vehicle data. 
 * Initially all vehicles are in the Stay mode at their initial stations.
 * 
 * @author balacm
 *
 */
@Singleton
public class UAMLoader implements BeforeMobsimListener {

	@Inject
	private UAMFleetData data;

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (Vehicle vehicle : data.getVehicles().values()) {
			vehicle.resetSchedule();

			Schedule schedule = vehicle.getSchedule();
			schedule.addTask(
					new UAMStayTask(vehicle.getServiceBeginTime(), Double.POSITIVE_INFINITY, vehicle.getStartLink())); // Usage of UAMStayTask
		}

	}

}
