package AmbulanceOptimization;
import java.util.LinkedList;
import java.util.Random;
import umontreal.ssj.randvar.ExponentialGen;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.simevents.Event;
import umontreal.ssj.simevents.Sim;

public class Region {
	LinkedList<Accident> queue;     //Queue of the server
	LinkedList<Ambulance> idleAmbulances;  // Available ambulance
	double [] baseLocation;
	ArrivalProcess arrivalProcess;
	RandomStream locationStream;
	int regionID;
	double dist = Math.sqrt((Math.pow(5, 2) - Math.pow(2.5, 2)));

	public Region(double baseXCoordinate, double baseYCoordinate, RandomStream rng, double arrivalRate, RandomStream location, int rid) {
		queue = new LinkedList<>();
		idleAmbulances = new LinkedList<>();
		baseLocation = new double[2];
		baseLocation[0] = baseXCoordinate;
		baseLocation[1] = baseYCoordinate;
		arrivalProcess = new ArrivalProcess(rng,arrivalRate);
		locationStream = location;
		regionID = rid;
	}

	public void handleArrival() {
		// process a new arrival
		Accident customer = new Accident(Sim.time(), drawLocation()[0], drawLocation()[1], regionID);

		if (!idleAmbulances.isEmpty()) {
			Ambulance ambulance = idleAmbulances.pop();
			ambulance.startService(customer, (Sim.time() + ambulance.drivingTimeToAccident(customer)));
		} else {
			queue.add(customer);
		}
	}

	public double[] drawLocation() {
		// determine the location of the accident
		double[] location = new double[2];
		Random rng = new Random();
		double[] coordinates = generateCoordinates(rng);
		
		if(!areCorrectCoordinates(coordinates)) {
			drawLocation();
		}

		location[0] = coordinates[0] + baseLocation[0]; // X-Coordinate of accident location
		location[1] = coordinates[1] + baseLocation[1]; // Y-Coordinate of accident location
		
		return location;
	}

	double[] generateCoordinates(Random rng) {
		//Generate random XY coordinates within a square with center point (0,0) and sides of length 10
		double[] coordinates = new double[2];
		int randomNumber1 = rng.nextInt(2);
		int randomNumber2 = rng.nextInt(2);
		double factor1 = locationStream.nextDouble();
		double factor2 = locationStream.nextDouble();

		if(randomNumber1 == 0) {
			coordinates[0] = -factor1 * 5;
		} else {
			coordinates[0] = factor1 * 5;
		}

		if(randomNumber2 == 0) {
			coordinates[1] = -factor2 * dist;
		} else {
			coordinates[1] = factor2 * dist;
		}
		return coordinates;
	}
	
	boolean areCorrectCoordinates(double[] coordinates) {
		//Check if coordinates lie within hectagonal boundaries
		double absXCoordinate = Math.abs(coordinates[0]);
		double absYCoordinate = Math.abs(coordinates[1]);
		
		if(absXCoordinate > 2.5) {
			if(absYCoordinate > (Math.sqrt(3) * absXCoordinate + 2 * dist)) {
				return false;
			}
		}
		return true;
	}

	class ArrivalProcess extends Event {
		ExponentialGen arrivalTimeGen;
		double arrivalRate;

		public ArrivalProcess(RandomStream rng, double arrivalRate) {
			this.arrivalRate = arrivalRate;
			arrivalTimeGen = new ExponentialGen(rng, arrivalRate);
		}

		@Override
		public void actions() {
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival);//Schedule this event after
			//nextArrival time units
			handleArrival();
		}

		public void init() {
			double nextArrival = arrivalTimeGen.nextDouble();
			schedule(nextArrival);//Schedule this event after
			//nextArrival time units
		}
	}

}



