package nycSubway;

public class Trip {
public
	String tripID;
	int arrival;
	int departure;
	String direction;
	
	String nextStopID;
	String nextTripID;
	int nextArrival;
	
	Trip(String tripID, int arrival, int departure, String direction, String nextStopID, String nextTripID, int nextArrival){
		this.tripID = tripID;
		this.arrival = arrival;
		this.departure = departure;
		this.direction = direction;
		this.nextStopID = nextStopID;
		this.nextTripID = nextTripID;
		this.nextArrival = nextArrival;
	}
}
