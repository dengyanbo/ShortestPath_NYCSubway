package nycSubway;

//import java.util.HashMap;

public class Stop {
public
	String ID;

	//int stopSquence;
	int stopTime;
	int transferTime;
	
	Stop(String name){
		this.ID = name;
	}
	Stop(String name, Integer time){
		this.ID = name;
		this.stopTime = time;
	}
	Stop(Integer transferTime, String name){
		this.ID = name;
		this.transferTime = transferTime;
	}
	
}
