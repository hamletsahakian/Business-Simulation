package AmbulanceOptimization;

import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;
import umontreal.ssj.stat.TallyStore;

public class Ambulance extends Event {
	Region baseRegion;
	Region[] regions;
	Accident currentCust; //Current customer in service
	double responseTime = 15.0;
	double distanceToBase =  2 * Math.sqrt((Math.pow(5, 2) - Math.pow(2.5, 2))); //Distance from hospital to ambulance waiting dock
	boolean serveOutsideRegion;
	ExponentialGen serviceTimeGen;
	TallyStore waitTimeTally = new TallyStore("Waittime");
	TallyStore serviceTimeTally = new TallyStore("Servicetime");
	TallyStore withinTargetTally = new TallyStore("Arrival within target");

	public Ambulance(Region baseRegion, RandomStream rng, double serviceTimeRate) {
		currentCust = null;
		this.baseRegion = baseRegion;
		serviceTimeGen = new ExponentialGen(rng, serviceTimeRate);
		serveOutsideRegion = false;
	}

	public Ambulance(Region baseRegion, RandomStream rng, double serviceTimeRate, boolean outside, Region[] regions) {
		currentCust = null;
		this.baseRegion = baseRegion;
		this.regions = regions;
		serviceTimeGen = new ExponentialGen(rng, serviceTimeRate);
		serveOutsideRegion = outside;
	}

	public void serviceCompleted(Ambulance amb, Accident currentCust) {
		// Process the completed `customer'
		if(amb.baseRegion.regionID != 0) {
			currentCust.completed(Sim.time() - distanceToBase);
		} else {
			currentCust.completed(Sim.time());
		}
		
		amb.waitTimeTally.add(currentCust.getWaitTime());
		amb.serviceTimeTally.add(currentCust.getServiceTime());
		if(currentCust.getWaitTime() < responseTime) {
			amb.withinTargetTally.add(currentCust.getWaitTime());
		}

		if(amb.baseRegion.idleAmbulances.isEmpty() && !amb.baseRegion.queue.isEmpty()) {
			Accident newCust = amb.baseRegion.queue.pop();
			amb.startService(newCust, (Sim.time() + amb.drivingTimeToAccident(newCust)));
		} else if (!amb.baseRegion.idleAmbulances.isEmpty() && serveOutsideRegion) {
			if(shortestDistanceAccident(amb) != null) {
				amb.startService(shortestDistanceAccident(amb), Sim.time());
			}
		} else {
			amb.baseRegion.idleAmbulances.add(amb);
		}
	}

	Accident shortestDistanceAccident(Ambulance amb) {
		double shortestDistance = 9999;
		int shortestAccidentIndex = 0;

		for(int i = 0; i < regions.length; i++) {
			if(!regions[i].queue.isEmpty() && regions[i].idleAmbulances.isEmpty() && amb.drivingTimeToAccident(regions[i].queue.element()) < shortestDistance) {
				shortestDistance = drivingTimeToAccident(regions[i].queue.element());
				shortestAccidentIndex = i;
			} else {
				return null;
			}
		}
		return regions[shortestAccidentIndex].queue.pop();
	}


	public double drivingTimeToAccident(Accident cust) {
		// calculate the driving time from the baselocation of the ambulance to the accident location

		double x1 = baseRegion.baseLocation[0];
		double y1 = baseRegion.baseLocation[1];

		double x2 = cust.getLocation()[0];
		double y2 = cust.getLocation()[1];

		return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
	}

	public double drivingTimeToHospital(Accident cust) {
		// calculate the driving time from accident location to the hospital
		double x1 = cust.getLocation()[0];
		double y1 = cust.getLocation()[1];

		double x2 = 0.0;
		double y2 = 0.0;

		return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1)) ;
	}

	@Override
	public void actions() {
		serviceCompleted(this, currentCust);
	}

	public void startService(Accident cust, double current) {
		currentCust = cust;
		cust.serviceStarted(current);

		double travelTimeAccident = drivingTimeToAccident(cust);
		double serviceTime = serviceTimeGen.nextDouble();
		double travelTimeHospital = drivingTimeToHospital(currentCust);
		double travelTimeBase;
		if(baseRegion.regionID == 0) {
			travelTimeBase = 0;
		} else {
			travelTimeBase = distanceToBase;
		}
		double busyServing = travelTimeAccident + serviceTime + travelTimeHospital + travelTimeBase; // Calculate the time needed to process the accident

		schedule(busyServing); //Schedule this event after serviceTime time units
	}
}
