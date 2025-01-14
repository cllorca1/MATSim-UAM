package net.bhl.matsim.uam.dispatcher;

import java.util.List;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Inject;

public class UAMDispatcherListener implements MobsimBeforeSimStepListener {

	@Inject
	List<Dispatcher> dispatchers;
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		for (Dispatcher d : dispatchers) {
			d.onNextTimeStep(e.getSimulationTime());
		}
		
	}

}
