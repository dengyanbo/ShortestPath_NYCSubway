package nycSubway;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NYC_Subway {
	static String StartStop = "Canal St";
	static String StartLine = "1";
	static String DestinationStop = "Harlem - 148 St";
	static String DestinationLine = "3";
	static String Date = "SUN";
	static String CurrentTime = "13:30:00";
	static String FilePath = "C:\\Users\\dengy\\workspace\\network\\src\\nycSubway\\google_transit\\";
	
	static int Infinite = 999999999;
	
	public static HashMap<String, String> findStops() throws FileNotFoundException, IOException{
		HashMap<String, String> Stops = new HashMap<String, String>();
		
		BufferedReader brStops = new BufferedReader(new FileReader(FilePath + "stops.txt"));
		String line;
		while ((line = brStops.readLine()) != null) {
		   ReadInData info = new ReadInData(line);
		   if(info.getParts(8).equals("1")){
			   String stopID = info.getParts(0);
			   String stopName = info.getParts(2);
			   Stops.put(stopID, stopName);
		   }
		}
		brStops.close();
		return Stops;
	}
	
	
	public static boolean findID(String ID, String SubwayLine, ArrayList<String> stopTimes) throws FileNotFoundException, IOException{
		boolean foundID = false;
		String theLine = "_" + SubwayLine + ".";

		for (String line: stopTimes) {
		   ReadInData info = new ReadInData(line);
		   if(info.getParts(3).equals(ID + "N") ||info.getParts(3).equals(ID + "S"))
			   if(info.getParts(0).contains(theLine) && info.getParts(0).contains(Date)){
				   foundID = true;
			   }
		}
		return foundID;
	}
	
	public static ArrayList<String> enrichStops(HashMap<String, String> Stops, ArrayList<String> stopTimes) throws IOException{
		ArrayList<String> stops = new ArrayList<String>();
		
		for (String line: stopTimes) {
			ReadInData info = new ReadInData(line);
			if(info.getParts(0).contains(Date)){
				String subwayLine = info.getParts(0).split("_")[2].split("\\.")[0];
				for(Map.Entry<String, String> stop: Stops.entrySet()){	   
				   String thisStop = stop.getKey() + "_" + subwayLine;
				   if(info.getParts(3).equals(stop.getKey() + "N") || info.getParts(3).equals(stop.getKey() + "S")){
					   if(!stops.contains(thisStop)){
						   stops.add(thisStop);
					   }
				   }
				}
			}
			
		}
		return stops;
	}
	
	private static HashMap<String, ArrayList<Trip>> stopTimesStructure(ArrayList<String> stops,
			ArrayList<String> stopTimes) {
		HashMap<String, ArrayList<Trip>> stopFile = new HashMap<String, ArrayList<Trip>>();
		int count = 0;
		for(String stop: stops){
			if(++count % 35 == 0)
				System.out.print(count/7 + "%");
			String[] parts = stop.split("_");
			String thisStop = parts[0];
			String thisLine = parts[1];
			ArrayList<Trip> stopTrip = new ArrayList<Trip>();
			for(int iter = 0; iter != stopTimes.size(); iter++){
				String line = stopTimes.get(iter);
				ReadInData info = new ReadInData(line);
				if(info.getParts(0).contains(Date)){
					if(info.getParts(3).equals(thisStop + "N")){
						String tripID = info.getParts(0);
						int arrival = getTime(info.getParts(1));
						int departure = getTime(info.getParts(2));
						String direction = "N";
						
						ReadInData infoNext = new ReadInData(stopTimes.get(iter + 1));
						String nextTripID = infoNext.getParts(0);
						String nextStopID = "";
						int nextArrival = -1;
						if(nextTripID.equals(tripID)){
							String temp = infoNext.getParts(3);
							nextStopID = temp.substring(0, temp.length() - 1) + "_" + thisLine;
							nextArrival = getTime(infoNext.getParts(1));
						}
						stopTrip.add(new Trip(tripID, arrival, departure, direction, nextStopID, nextTripID, nextArrival));
					}else if(info.getParts(3).equals(thisStop + "S")){
						String tripID = info.getParts(0);
						int arrival = getTime(info.getParts(1));
						int departure = getTime(info.getParts(2));
						String direction = "S";

						ReadInData infoNext = new ReadInData(stopTimes.get(iter + 1));
						String nextTripID = infoNext.getParts(0);
						String nextStopID = "";
						int nextArrival = -1;
						if(nextTripID.equals(tripID)){
							String temp = infoNext.getParts(3);
							nextStopID = temp.substring(0, temp.length() - 1) + "_" + thisLine;
							nextArrival = getTime(infoNext.getParts(1));
						}
						stopTrip.add(new Trip(tripID, arrival, departure, direction, nextStopID, nextTripID, nextArrival));
					}
				}
			}
			stopFile.put(stop, stopTrip);
		}
		
		return stopFile;
	}

	
	public static boolean dijkstra(ArrayList<String> stops, String startID, String startLine, String destinationID, String destinationLine, 
			HashMap<String, String> Stops, HashMap<String, ArrayList<Trip>> stopFile) throws FileNotFoundException, IOException{
		String[] parts = CurrentTime.split(":");
		int currentTime = Integer.parseInt(parts[2]) + 60 * (Integer.parseInt(parts[1]) + 60 * Integer.parseInt(parts[0]));
		int startTime = currentTime;
		
		ArrayList<String> transfersFile = new ArrayList<String>();
		ArrayList<String> sptSet = new ArrayList<String>();//shortest path tree set
		ConcurrentHashMap<String, String> preStop = new ConcurrentHashMap<String, String>();//key is the current stop, value is previous stop in shortest path tree
		HashMap<String, Integer> distance = new HashMap<String, Integer>();//all stops with their min distances to their source
		
		BufferedReader brTransfers = new BufferedReader(new FileReader(FilePath + "transfers.txt"));
		String line;
		while ((line = brTransfers.readLine()) != null) {
		   transfersFile.add(line);
		}
		brTransfers.close();

		int stopNumber = stops.size();
		System.out.println("Start from Line " + StartLine + " " + StartStop);
		System.out.println("Finding the best route to Line " + DestinationLine + " " + DestinationStop);
		for(Map.Entry<String, ArrayList<Trip>> stop: stopFile.entrySet()){
			distance.put(stop.getKey(), Infinite);
		}
		distance.put(startID + "_" + startLine, currentTime);
		preStop.put(startID + "_" + startLine, startID + "_" + startLine);//the pre-stop of source is itself
		
		int count = 0;
		while(sptSet.size() != stopNumber){
			if(count++ % 10 == 0 )
				System.out.print(".");
			
			//**********extract the min value from sptSet*************
			int minValue = Infinite;//initially set min value to infinite
			String cur = null;//default cur
			for(Map.Entry<String, Integer> entry: distance.entrySet()){
				if(!sptSet.contains(entry.getKey())){
					if(minValue >= entry.getValue() + distance.get(entry.getKey())){
						minValue = entry.getValue() + distance.get(entry.getKey());
						cur = entry.getKey();
					}
				}
			}
			//System.out.print("current Stop is: " + cur + " time is: ");
			//displayTime(currentTime);
			if(cur != null){
				sptSet.add(cur);
				currentTime = distance.get(cur);
				if(cur.equals(destinationID + "_" + DestinationLine)){
					System.out.println("DONE!\n");
					break;
				}
			}
			else
				return false;
			//********************************************************
			
			//-------------update distances -----------------------
			//go through the neighbors
			HashMap<String, Stop> foundStops = findStopTimes(cur, stopFile, currentTime);
			Stop curStop = foundStops.get("this");
			Stop nextStopN = foundStops.get("N");
			Stop nextStopS = foundStops.get("S");
			assert(!curStop.ID.equals(""));
			
			if(nextStopN != null){
				if(!sptSet.contains(nextStopN.ID)){
					if(distance.containsKey(nextStopN.ID))
					if(distance.get(nextStopN.ID) > nextStopN.stopTime){
						distance.put(nextStopN.ID, nextStopN.stopTime);
						preStop.put(nextStopN.ID, cur);
						//System.out.println("North nextStop ID: " + nextStop.ID + " time " + nextStop.stopTime);
					}
				}
			}
			if(nextStopS != null){
				if(!sptSet.contains(nextStopS.ID)){
					if(distance.containsKey(nextStopS.ID))
					if(distance.get(nextStopS.ID) > nextStopS.stopTime){
						distance.put(nextStopS.ID, nextStopS.stopTime);
						preStop.put(nextStopS.ID, cur);
						//System.out.println("North nextStop ID: " + nextStop.ID + " time " + nextStop.stopTime);
					}
				}
			}
			
			//if there are transfer neighbors
			ArrayList<Stop> transferStops = findTransferStops(curStop, currentTime, stopFile, transfersFile);
			for(Stop transferStop: transferStops){
				if(transferStop.ID != ""){
					if(!sptSet.contains(transferStop.ID)){
						if(distance.get(transferStop.ID) > currentTime + transferStop.transferTime){
							distance.put(transferStop.ID, currentTime + transferStop.transferTime);
							preStop.put(transferStop.ID, cur);
							//System.out.println("Transfer Stop ID: " + transferStop.ID + " time " + transferStop.transferTime);
						}
					}
				}
			}
			//end updating-------------------------------------------
		}
		
		System.out.println("The The shortest path from Line " + StartLine + " " + StartStop + " to Line " + DestinationLine + " " + DestinationStop + " is:");
		System.out.print(" Line " + StartLine + " " +StartStop);
		display(destinationID + "_" + DestinationLine, startID + "_" + StartLine, preStop, Stops);
		System.out.print("\nThe approximate time of arrival is: ");
		displayTime(distance.get(destinationID + "_" + DestinationLine));
		System.out.print("The approximate time cost is: ");
		displayTime(distance.get(destinationID + "_" + DestinationLine) - startTime);
		
		return true;
	}
	
	private static void display(String destinationID, String startID, ConcurrentHashMap<String, String> preStop, HashMap<String, String> stops) {
		if(!destinationID.equals(startID)){
			String dID = destinationID.split("_")[0];
			String dLine = destinationID.split("_")[1];
			String preLine = preStop.get(destinationID).split("_")[1];
			display(preStop.get(destinationID), startID, preStop, stops);
			if(dLine.equals(preLine)){
				System.out.print("\n -> " + stops.get(dID));
			}else{
				System.out.print("\n Transfer to Line " + dLine + "\n -> " + stops.get(dID));
			}
		}
	}
	
	private static void displayTime(int time){
		int second = time % 60;
		int minute = (time / 60) % 60;
		int hour = (time / 60) / 60;
		String Second = (second < 10? "0" : "") + second;
		String Minute = (minute < 10? "0" : "") + minute;
		String Hour = (hour < 10? "0" : "") + hour;
		System.out.println(Hour + ":" + Minute + ":" + Second);
	}

	private static int getTime(String stringTime) {
		String[] parts = stringTime.split(":");
		int hour = Integer.parseInt(parts[0]);
		int minute = Integer.parseInt(parts[1]);
		int second = Integer.parseInt(parts[2]);
		int time = second + 60 * (minute + 60 * hour);
		return time;
	}
	
	private static ArrayList<Stop> findTransferStops(Stop curStop, int currentTime, HashMap<String, ArrayList<Trip>> stopFile, ArrayList<String> transfersFile) throws FileNotFoundException, IOException {
		ArrayList<Stop> transferStops = new ArrayList<Stop>();
		ArrayList<Stop> transferIDs = new ArrayList<Stop>();
		
		String[] temp_parts = curStop.ID.split("_");
		String thisStopID = temp_parts[0];
		
		//get the transferIDs: the stop IDs that curStop can transfer stop to
		for(String line: transfersFile) {
		   ReadInData info = new ReadInData(line);
		   if(info.getParts(0).equals(thisStopID)){
			   String transferStopID = info.getParts(1);
			   int transferTime = Integer.parseInt(info.getParts(3));
			   transferIDs.add(new Stop(transferTime, transferStopID));
		   }
		}		

		//get the transferStops: which stops(include its subway line) are the transfer stops for our curStop
		//two possibility: curStop to curStop in different line
		//				   curStop to another stop in different line
		
		for(Map.Entry<String, ArrayList<Trip>> stopTrip: stopFile.entrySet()){
			String stopID = stopTrip.getKey().split("_")[0];
			for(Stop transferID: transferIDs){
				if(stopID.equals(transferID.ID)){
					Stop newStop = new Stop(transferID.transferTime, stopTrip.getKey());
					
					boolean isNew = true;
					for(Stop astop: transferStops){
						if(astop.ID.equals(stopTrip.getKey())){
							isNew = false;
						}
					}
					if(isNew) transferStops.add(newStop);
				}
			}
			
		}
		/*
		for (String oneLine: stopTimes){
			ReadInData info = new ReadInData(oneLine);
			for(Stop transferID: transferIDs){
				if(info.getParts(0).contains(Date) && (info.getParts(3).equals(transferID.ID + "N") || info.getParts(3).equals(transferID.ID + "S"))){
					if(!info.getParts(0).contains("_" + subwayLine +".")){
						String thisSubwayLine = info.getParts(0).split("_")[2].split("\\.")[0];
						String transferStop = transferID.ID + "_" + thisSubwayLine;
						Stop thisStop = new Stop(transferID.transferTime, transferStop);
						
						boolean newStop = true;
						for(Stop astop: transferStops){
							if(astop.ID.equals(transferStop)){
								newStop = false;
							}
						}
						if(newStop){
							transferStops.add(thisStop);
						}
					}
				}
				
			}
		}
		*/
		return transferStops;
	}
	
	public static HashMap<String, Stop> findStopTimes(String stopID, HashMap<String, ArrayList<Trip>> stopFile, Integer currentTime) throws FileNotFoundException, IOException{
		HashMap<String, Stop> foundStops = new HashMap<String, Stop>();
		Stop stop = new Stop(stopID);
		Stop nextStopN = null;
		Stop nextStopS = null;
		
		int minTimeN = Infinite;
		int minTimeS = Infinite;
		
		ArrayList<Trip> stopTrip = stopFile.get(stopID);
		for(Trip trip: stopTrip){
			if(!trip.nextStopID.equals("")){
				if(trip.direction.equals("N")){
					if(trip.departure >= currentTime && minTimeN >= trip.departure){
						minTimeN = trip.departure;
						nextStopN = new Stop(trip.nextStopID, trip.nextArrival);
					}
				}else if(trip.direction.equals("S")){
					if(trip.departure >= currentTime && minTimeS >= trip.departure){
						minTimeS = trip.departure;
						nextStopS = new Stop(trip.nextStopID, trip.nextArrival);
					}
				}else System.err.println("trip direction error!");
			}
		}

		foundStops.put("this", stop);
		foundStops.put("N", nextStopN);
		foundStops.put("S", nextStopS);
		
		return foundStops;
	}
	
	public static void main(String arg[]) throws FileNotFoundException, IOException{
		String startID = "";
		String destinationID = "";
		
		System.out.println("Initiallizing System...");
		
		//load stop times from file
		System.out.print("Loading input information...");
		ArrayList<String> stopTimes = new ArrayList<String>();
		BufferedReader brStopTimes = new BufferedReader(new FileReader(FilePath + "stop_times.txt"));
		String line;
		while ((line = brStopTimes.readLine()) != null) {
			stopTimes.add(line);
		}
		brStopTimes.close();
		System.out.println("DONE");

		System.out.print("Loading data from file...");
		HashMap<String, String> Stops = findStops();//key: the unique ID of stops, value: the name of stops
		//find the unique ID of start and destination
		for(Map.Entry<String, String> entry: Stops.entrySet()){
			if(entry.getValue().equals(StartStop)){
				//System.out.println("start here: " + entry.getKey() + " " + entry.getValue());
				if(findID(entry.getKey(), StartLine, stopTimes)){
					startID = entry.getKey();
					//System.out.println("Start ID is: " + startID);
				}
			}
			if(entry.getValue().equals(DestinationStop)){
				//System.out.println("destination here: " + entry.getKey() + " " + entry.getValue());
				if(findID(entry.getKey(), DestinationLine, stopTimes)){
					destinationID = entry.getKey();
					//System.out.println("Destination ID is: " + destinationID);
				}
			}
		}//have found the IDs
		
		ArrayList<String> stops = enrichStops(Stops, stopTimes);
		System.out.println("DONE");
		
		System.out.print("Building subway structure......");
		HashMap<String, ArrayList<Trip>> stopTimeFile = stopTimesStructure(stops, stopTimes);
		System.out.println("DONE\n");
		
		if(dijkstra(stops, startID, StartLine, destinationID, DestinationLine, Stops, stopTimeFile))
			System.out.println("\nThanks for using our system!");
		else
			System.out.println("System error!");
		
	}
}
