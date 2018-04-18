package nycSubway;

public class ReadInData {
	String input;
	String[] parts;
	int PartSize;
	ReadInData(String input){
		this.input = input;
		this.parts = input.split(",");
		this.PartSize = parts.length;
	}
	public int getPartSize(){
		return PartSize;
	}
	public String getParts(int i){
		assert(i < PartSize);
		return parts[i];
	}
}
