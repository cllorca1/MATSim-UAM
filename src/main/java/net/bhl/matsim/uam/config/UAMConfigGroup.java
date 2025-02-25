package net.bhl.matsim.uam.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

import net.bhl.matsim.uam.router.UAMIntermodalRoutingModule;
import net.bhl.matsim.uam.router.strategy.UAMStrategy;
//import net.bhl.matsim.uam.router.UAMStrategyRouter.UAMStrategyType;
import net.bhl.matsim.uam.router.strategy.UAMStrategy.UAMStrategyType;

/**
 * 
 * @author balacm
 *
 */
public class UAMConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "uam";

	private String inputUAMFile;
	private Set<String> availableAccessModes;
	
	private UAMStrategyType routingStrategy = UAMStrategyType.MAXACCESSUTILITY;
	
	private int parallelRouters = 2;
	private double searchRadius = 5000; // maximum crow fly distance to origin/destination stations
	private double walkDistance = 500; // if the access/egress distance is less than walkDistance, then walk will be the uam access and egress mode, otherwise the fastest (car or pt)

	private boolean ptSimulation = true; // selects whether public transport will be simulated or performed by teleportation
	private static final Logger log = Logger.getLogger(UAMConfigGroup.class);

	public UAMConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter("inputUAMFile")
	public String getUAM() {
		return this.inputUAMFile;
	}

	@StringSetter("inputUAMFile")
	public void setUAM(final String inputUAMFile) {
		this.inputUAMFile = inputUAMFile;
	}
	
	@StringGetter("routingStrategy")
	public UAMStrategyType getUAMRoutingStrategy() {
		return this.routingStrategy;
	}

	@StringSetter("routingStrategy")
	public void setUAMRoutingStrategy(final String routingStrategy) {
		try {
			this.routingStrategy = UAMStrategyType.valueOf(routingStrategy.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.warn("Unknown UAM routing strategy: " + routingStrategy + "; Possible strategies are:");
			int i = 0;
			for (UAMStrategyType st : UAMStrategy.UAMStrategyType.values())
				log.warn(i++ + ". " + st.toString());
		
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	@StringGetter("parallelRouters")
	public int getParallelRouters() {
		return this.parallelRouters;
	}

	@StringSetter("parallelRouters")
	public void setParallelRouters(final String parallelRouters) {
		this.parallelRouters = Integer.parseInt(parallelRouters);
	}

	@StringGetter("searchRadius")
	public double getSearchRadius() {
		return this.searchRadius;
	}

	@StringSetter("walkDistance")
	public void setWalkDistance(final String walkDistance) {
		this.walkDistance = Double.parseDouble(walkDistance);
	}
	
	@StringGetter("walkDistance")
	public double getWalkDistance() {
		return this.walkDistance;
	}

	@StringSetter("searchRadius")
	public void setSearchRadius(final String searchRadius) {
		this.searchRadius = Double.parseDouble(searchRadius);
	}
	
	@StringGetter("ptSimulation")
	public boolean getPtSimulation() {
		return this.ptSimulation;
	}
	
	@StringSetter("ptSimulation")
	public void setPtSimulation(final String ptSimulation) {
		if (Boolean.parseBoolean(ptSimulation)) {
			log.warn("In case of simulating PT do not add \"pt\" as a parameterset type=\"teleportedModeParameteres\" in the planscalcroute module!");
		} else {
			log.warn("You chose to not simulate PT, please add \"pt\" as a parameterset type=\"teleportedModeParameteres\" in the planscalcroute module!");
		}
		this.ptSimulation = Boolean.parseBoolean(ptSimulation);
	}

	@StringGetter("availableAccessModes")
	public Set<String> getAvailableAccessModes() {
		return this.availableAccessModes;
	}

	@StringSetter("availableAccessModes")
	public void setAvailableAccessModes(final String availableAccessModes) {
		String[] arr = availableAccessModes.split(",");
		this.availableAccessModes = new HashSet<>();
		this.availableAccessModes.addAll(Arrays.asList(arr));
	}
}
