import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;

public class Util {
	public static final int SECONDS_IN_A_DAY = 86400;
	
	// indices for accessing the location arrays in vehiclePaths (see makeVehicleArray())
	public static final int ID = 0;
	public static final int TIME = 1;
	public static final int LON = 2;
	public static final int LAT = 3;
	public static final int REGION = 4;
	
	public static boolean isSamePoint(double[] point1, double[] point2){
		return (point1[0] == point2[0]) && (point1[1] == point2[1]);
	}
	
	public static double distance(double[] p1, double[] p2){
		return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
	}
	
	/* 
	 * Created using the haversine formua found here:
	 * http://www.movable-type.co.uk/scripts/latlong.html
	 */
	public static double distanceKm(double[] p1, double[] p2){
		// convert to radians
		double[] p1Rad = {Math.toRadians(p1[0]), Math.toRadians(p1[1])};
		double[] p2Rad = {Math.toRadians(p2[0]), Math.toRadians(p2[1])};
		
		double deltaLon = Math.abs(p1Rad[0] - p2Rad[0]);
		double deltaLat = Math.abs(p1Rad[1] - p2Rad[1]);
		double deltaLonHalf = deltaLon / 2.0;
		double deltaLatHalf = deltaLat / 2.0;
		double cosLat1 = Math.cos(p1Rad[1]);
		double cosLat2 = Math.cos(p2Rad[1]);
		double R = 6371.0;
		
		double a = Math.sin(deltaLatHalf) * Math.sin(deltaLatHalf)
				+ cosLat1 * cosLat2 * Math.sin(deltaLonHalf) * Math.sin(deltaLonHalf);
		double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = R * c;
		
		return d;
	}
	
	public static double getLatestTime(String filename) throws IOException{
		BufferedReader f = new BufferedReader(new FileReader(filename));
		String line = "";
		double latestTime = -1;
		while ((line = f.readLine()) != null) {
			if(line.split(",").length == 4){
				latestTime = Math.max(latestTime, Double.parseDouble(line.split(",")[TIME]));
			}
		}
		f.close();
		
		return latestTime;
	}
	
	public static int getLastVehicleId(String filename) throws IOException{
		BufferedReader f = new BufferedReader(new FileReader(filename));
		String lastLine = "";
		String line = "";
		while ((line = f.readLine()) != null) {
			lastLine = (line.indexOf(',') == -1) ? lastLine : line;
		}
		int lastVehicleId = Integer.parseInt(lastLine.split(",")[ID]);
		f.close();
		
		return lastVehicleId;
	}
	
	public static double[] toDoubleArray(JSONArray j){
		double[] d = new double[j.size()];
		for(int i = 0; i < j.size(); i++){
			d[i] = (double) j.get(i);
		}
		
		return d;
	}
	
	public static double[] getXY(JSONArray points){
		double[] x = new double[points.size() * 2];
		for(int i = 0, j = 0; i < points.size(); i++, j += 2){
			JSONArray point = (JSONArray) points.get(i);
			x[j] = (double) point.get(0);
			x[j + 1] = (double) point.get(1);
		}
		
		return x;
	}
}
