package net.bhl.matsim.uam.data;

import net.bhl.matsim.uam.infrastructure.UAMStation;

public class UAMRoute {
	public String accessMode;
	public UAMStation bestOriginStation;
	public UAMStation bestDestinationStation;
	public String egressMode;

	public UAMRoute(String accessMode, UAMStation bestOriginStation, UAMStation bestDestinationStation, String egressMode) {
		this.accessMode = accessMode;
		this.bestOriginStation = bestOriginStation;
		this.bestDestinationStation = bestDestinationStation;
		this.egressMode = egressMode;
	}
}
