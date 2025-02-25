package net.bhl.matsim.uam.dispatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import net.bhl.matsim.uam.infrastructure.UAMStation;
import net.bhl.matsim.uam.infrastructure.UAMStations;
import net.bhl.matsim.uam.infrastructure.UAMVehicle;

public class UAMManager implements IterationStartsListener {

	private Set<UAMVehicle> availableVehicles = new HashSet<>();
	public QuadTree<UAMVehicle> mapAvailableVehicles;
	private Map<Id<Vehicle>, Id<UAMStation>> vehicleLocations = new HashMap<>();;
	private UAMStations stations;
//	private Set<UAMVehicle> vehicles;
	private Map<Id<Vehicle>, UAMVehicle> vehicles = new HashMap<Id<Vehicle>, UAMVehicle>(); //added
	private Map<Id<UAMStation>, StationOccupancy> availablespaceStations = new HashMap<>();
	private QuadTree<UAMStation> stationsWithFreeLandingSpace;

	private Map<Id<Person>, UAMVehicle> vehiclePersonmap = new HashMap<>();
	private Map<Id<Person>, UAMStation> stationPersonmap = new HashMap<>();

	private Network network;

	public UAMManager(Network network) {
		this.network = network;
	}

	public void setStations(UAMStations stations) {
		this.stations = stations;
	}

	public UAMStations getStations() {
		return stations;
	}

	public Map<Id<Vehicle>, UAMVehicle> getVehicles() {
		return vehicles;
	}

	public void setVehicles(Map<Id<Vehicle>, UAMVehicle> vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * 
	 * Adds a vehicle, after it lands, to the available vehicles.
	 * 
	 */
	public synchronized void addVehicle(UAMVehicle vehicle, UAMStation station) {

		Coord coord = station.getLocationLink().getCoord();
		if (!this.availableVehicles.add(vehicle))
			throw new RuntimeException("The system is in incosistent state! \n "
					+ "Trying to add a vehicle but it is already there!");	
		this.mapAvailableVehicles.put(coord.getX(), coord.getY(), vehicle);
		this.vehicleLocations.put(vehicle.getId(), station.getId());
		
	}


	public synchronized void removeVehicle(UAMVehicle vehicle, UAMStation ls) {
		if (!ls.getId().equals(this.vehicleLocations.get(vehicle.getId())))

			throw new RuntimeException("The system is in incosistent state! \n "
					+ "Trying to remove a vehicle from a station where it is not actually located!");		

		this.availablespaceStations.get(ls.getId()).landingSpace++;
		Coord coord = ls.getLocationLink().getCoord();
		if (!this.stationsWithFreeLandingSpace.getDisk(coord.getX(), coord.getY(), 0.0).contains(ls))
			this.stationsWithFreeLandingSpace.put(coord.getX(), coord.getY(), ls);

	}

	public synchronized UAMVehicle getClosestAvailableVehicle(Coord coord) {
		return this.mapAvailableVehicles.getClosest(coord.getX(), coord.getY());

	}

	public synchronized void reserveVehicle(Id<Person> personId, UAMVehicle vehicle) {
		if(!this.availableVehicles.remove(vehicle))
				throw new RuntimeException("The system is in incosistent state! \n "
						+ "Trying to remove a vehicle, but it was already removed!");		
		this.availableVehicles.remove(vehicle);
		UAMStation station = this.stations.getUAMStations().get(this.vehicleLocations.get(vehicle.getId()));
		Coord coord = station.getLocationLink().getCoord();
		if (!this.mapAvailableVehicles.remove(coord.getX(), coord.getY(), vehicle))
			throw new RuntimeException("The system is in incosistent state! \n "
					+ "Trying to remove a vehicle, but it was already removed!");
		this.vehiclePersonmap.put(personId, vehicle);
	}

	public UAMVehicle getReservedVehicle(Id<Person> personId) {
		return this.vehiclePersonmap.get(personId);
	}

	public UAMStation getReservedStation(Id<Person> personId) {
		return this.stationPersonmap.get(personId);
	}

	public synchronized UAMStation getClosestStationWithLandingSpace(Coord coord) {
		return this.stationsWithFreeLandingSpace.getClosest(coord.getX(), coord.getY());
	}

	public synchronized UAMStation getLSWHereVehicleIs(UAMVehicle vehicle) {
		Map<Id<UAMStation>, UAMStation> mappedLS = this.stations.getUAMStations();

		return mappedLS.get(this.vehicleLocations.get(vehicle.getId()));
	}

	private class StationOccupancy {

		int landingSpace;
		//int parkingSpace;
	}

	/**
	 * Initialize all datasets.
	 * 
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values()); // minx, miny, maxx, maxy

		mapAvailableVehicles = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		stationsWithFreeLandingSpace = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
		availableVehicles = new HashSet<>();

		vehicleLocations = new HashMap<>();
		availablespaceStations = new HashMap<>();
		vehiclePersonmap = new HashMap<>();
		stationPersonmap = new HashMap<>();

		for (UAMStation ls : stations.getUAMStations().values()) {

			StationOccupancy so = new StationOccupancy();
			so.landingSpace = ls.getLandingCapacity();
			//so.parkingSpace = ls.getParkingCapacity();

			availablespaceStations.put(ls.getId(), so);
			stationsWithFreeLandingSpace.put(ls.getLocationLink().getCoord().getX(),
					ls.getLocationLink().getCoord().getY(), ls);
		}
		
		
		for (UAMVehicle vehicle : vehicles.values()) {
			UAMStation station = this.stations.getUAMStations().get((vehicle).getInitialStationId());
			this.availablespaceStations.get(station.getId()).landingSpace--;
			if (availablespaceStations.get(station.getId()).landingSpace == 0)
				this.stationsWithFreeLandingSpace.remove(station.getLocationLink().getCoord().getX(),
						station.getLocationLink().getCoord().getY(), station);
			else if (availablespaceStations.get(station.getId()).landingSpace < 0)
				throw new RuntimeException("The system is an incosistent state! \n "
						+ "Trying to add a vehicle on a station where there is no more space left! Check your input configuration!");

			this.vehicleLocations.put(vehicle.getId(), station.getId());
			this.availableVehicles.add(vehicle);

			Link stationLink = station.getLocationLink();
			this.mapAvailableVehicles.put(stationLink.getCoord().getX(), stationLink.getCoord().getY(), vehicle);
		}

	}

	public synchronized void reserveLandingSpot(Id<Person> personId, UAMStation destStation) {
		if (this.availablespaceStations.get(destStation.getId()).landingSpace <= 0)
			throw new RuntimeException("The system is an incosistent state! \n "
					+ "Trying to reserve a spot at a station where there is no space!");
		this.availablespaceStations.get(destStation.getId()).landingSpace--;
		if (this.availablespaceStations.get(destStation.getId()).landingSpace == 0)
			this.stationsWithFreeLandingSpace.remove(destStation.getLocationLink().getCoord().getX(),
					destStation.getLocationLink().getCoord().getY(), destStation);

		this.stationPersonmap.put(personId, destStation);

	}

}
