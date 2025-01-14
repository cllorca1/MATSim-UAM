package net.bhl.matsim.uam.transit.simulation;

import java.util.Collection;
import java.util.Collections;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;

public class UAMTransitPlugin extends AbstractQSimPlugin {
	public UAMTransitPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Provides
			@Singleton
			public UAMTransitEngine provideBaselineTransitEngine(EventsManager eventsManager,
					TransitSchedule transitSchedule, DepartureFinder departureFinder, QSim qsim) {
				return new UAMTransitEngine(eventsManager, transitSchedule, departureFinder, qsim.getAgentCounter());
			}
		});
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonList(UAMTransitEngine.class);
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.singletonList(UAMTransitEngine.class);
	}
}
